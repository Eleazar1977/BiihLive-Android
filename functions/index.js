const {onCall, onRequest, HttpsError} = require('firebase-functions/v2/https');
const {onSchedule} = require('firebase-functions/v2/scheduler');
const {initializeApp} = require('firebase-admin/app');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const {getAuth} = require('firebase-admin/auth');
const nodemailer = require('nodemailer');

initializeApp();

// Configurar Firestore para usar la base de datos "basebiihlive"
const db = getFirestore();
db.settings({databaseId: 'basebiihlive'});

// Configuración de AWS SES desde variables de entorno
const AWS_SES_CONFIG = {
  host: process.env.AWS_SES_SMTP_HOST || 'email-smtp.eu-west-1.amazonaws.com',
  port: parseInt(process.env.AWS_SES_SMTP_PORT || '587'),
  secure: false,
  auth: {
    user: process.env.AWS_SES_SMTP_USER,
    pass: process.env.AWS_SES_SMTP_PASSWORD
  }
};

// Configuración de email
const EMAIL_CONFIG = {
  FROM_EMAIL: process.env.AWS_SES_FROM_EMAIL || 'noreply@biihlive.com',
  FROM_NAME: process.env.AWS_SES_FROM_NAME || 'BiihLive',
  CODE_EXPIRY_MINUTES: 10,
  MAX_ATTEMPTS: 3
};

// Crear transporter
let transporter = null;
if (AWS_SES_CONFIG.auth.user && AWS_SES_CONFIG.auth.pass) {
  transporter = nodemailer.createTransport(AWS_SES_CONFIG);
  console.log('✅ AWS SES transporter configurado correctamente');
} else {
  console.warn('⚠️ AWS SES credentials no configuradas');
}

/**
 * Generar código de verificación de 6 dígitos
 */
function generateVerificationCode() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

/**
 * Template HTML para el email
 */
function getEmailTemplate(code, userName) {
  return `
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Código de Verificación - BiihLive</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            background-color: #f5f5f5;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            padding: 20px 16px;
        }
        .header {
            text-align: center;
            padding-bottom: 12px;
            border-bottom: 2px solid #2196F3;
            margin-bottom: 16px;
        }
        .logo {
            font-size: 24px;
            font-weight: bold;
            color: #2196F3;
            margin-bottom: 4px;
        }
        .title {
            font-size: 20px;
            color: #333333;
            margin: 12px 0 8px 0;
        }
        .code-container {
            background-color: #f8f9fa;
            border: 2px solid #2196F3;
            border-radius: 8px;
            padding: 16px;
            text-align: center;
            margin: 12px 0;
        }
        .code {
            font-size: 32px;
            font-weight: bold;
            color: #2196F3;
            letter-spacing: 6px;
            margin: 8px 0;
        }
        .expiry {
            color: #666;
            font-size: 13px;
            margin-top: 6px;
        }
        .warning {
            background-color: #fff3cd;
            border-left: 3px solid #ffc107;
            border-radius: 4px;
            padding: 10px 12px;
            margin: 12px 0;
            color: #856404;
            font-size: 13px;
            line-height: 1.4;
        }
        .footer {
            border-top: 1px solid #eeeeee;
            padding-top: 12px;
            margin-top: 16px;
            color: #999999;
            font-size: 12px;
            text-align: center;
            line-height: 1.3;
        }
        p {
            margin: 8px 0;
            color: #666666;
            font-size: 14px;
            line-height: 1.4;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="logo">📱 BiihLive</div>
            <div style="color: #666; font-size: 14px;">Verificación de Email</div>
        </div>

        <h1 class="title">¡Hola ${userName || 'Usuario'}!</h1>

        <p>Usa este código para verificar tu cuenta:</p>

        <div class="code-container">
            <div class="code">${code}</div>
            <div class="expiry">Expira en ${EMAIL_CONFIG.CODE_EXPIRY_MINUTES} minutos</div>
        </div>

        <div class="warning">
            <strong>⚠️ Importante:</strong> Válido por ${EMAIL_CONFIG.CODE_EXPIRY_MINUTES} min • No lo compartas • Ignora si no lo solicitaste
        </div>

        <p style="font-size: 13px;">Ingresa este código en la app BiihLive. Si tienes problemas, contacta soporte.</p>

        <div class="footer">
            Este email fue enviado por BiihLive<br>
            Si no solicitaste esta verificación, ignora este mensaje
        </div>
    </div>
</body>
</html>
  `;
}

/**
 * Enviar código de verificación por email
 */
exports.sendEmailVerificationCode = onCall(async (request) => {
  const {email, userId} = request.data;

  console.log(`📧 Iniciando envío de código para: ${email}, userId: ${userId}`);

  // Validaciones
  if (!email || !userId) {
    throw new HttpsError('invalid-argument', 'Email y userId son requeridos');
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    throw new HttpsError('invalid-argument', 'Email inválido');
  }

  try {
    const now = new Date();
    const expiresAt = new Date(now.getTime() + EMAIL_CONFIG.CODE_EXPIRY_MINUTES * 60 * 1000);

    // Generar código
    const verificationCode = generateVerificationCode();
    console.log(`🔢 Código generado: ${verificationCode}`);

    // Obtener información del usuario para personalización
    let userName = 'Usuario';
    try {
      const userDoc = await db.collection('users').doc(userId).get();
      if (userDoc.exists) {
        const userData = userDoc.data();
        userName = userData.nickname || userData.displayName || 'Usuario';
      }
    } catch (error) {
      console.warn('No se pudo obtener info del usuario:', error);
    }

    // Guardar código en Firestore
    await db.collection('emailVerification').doc(userId).set({
      email: email,
      code: verificationCode,
      createdAt: FieldValue.serverTimestamp(),
      expiresAt: expiresAt,
      verified: false,
      attempts: 0,
      userId: userId
    });
    console.log(`💾 Código guardado en Firestore`);

    // Enviar email con AWS SES
    if (transporter) {
      const mailOptions = {
        from: `"${EMAIL_CONFIG.FROM_NAME}" <${EMAIL_CONFIG.FROM_EMAIL}>`,
        to: email,
        subject: `${verificationCode} - Tu código de verificación BiihLive`,
        html: getEmailTemplate(verificationCode, userName)
      };

      await transporter.sendMail(mailOptions);
      console.log(`✅ Email enviado exitosamente a ${email}`);
    } else {
      console.error('❌ Transporter no configurado - no se puede enviar email');
      throw new HttpsError('internal', 'Servicio de email no configurado');
    }

    return {
      success: true,
      message: 'Código enviado exitosamente',
      expiresAt: expiresAt.toISOString()
    };

  } catch (error) {
    console.error('❌ Error enviando código de verificación:', error);
    if (error instanceof HttpsError) {
      throw error;
    }
    throw new HttpsError('internal', `Error interno: ${error.message}`);
  }
});

/**
 * Verificar código de email
 */
exports.verifyEmailCode = onCall(async (request) => {
  const {userId, code} = request.data;

  console.log(`🔍 Verificando código para userId: ${userId}`);

  // Validaciones
  if (!userId || !code) {
    throw new HttpsError('invalid-argument', 'UserId y código son requeridos');
  }

  if (code.length !== 6 || !/^\d{6}$/.test(code)) {
    throw new HttpsError('invalid-argument', 'El código debe ser de 6 dígitos');
  }

  try {
    const auth = getAuth();

    // Obtener datos de verificación
    const verificationDoc = await db.collection('emailVerification').doc(userId).get();

    if (!verificationDoc.exists) {
      throw new HttpsError('not-found', 'Código de verificación no encontrado');
    }

    const verificationData = verificationDoc.data();
    const now = new Date();

    // Verificar expiración
    if (verificationData.expiresAt.toDate() < now) {
      throw new HttpsError('deadline-exceeded', 'El código ha expirado');
    }

    // Verificar intentos máximos
    if (verificationData.attempts >= EMAIL_CONFIG.MAX_ATTEMPTS) {
      throw new HttpsError('resource-exhausted', 'Demasiados intentos fallidos');
    }

    // Verificar si ya fue verificado
    if (verificationData.verified) {
      throw new HttpsError('already-exists', 'Este email ya fue verificado');
    }

    // Verificar el código
    if (verificationData.code !== code) {
      // Incrementar intentos fallidos
      await db.collection('emailVerification').doc(userId).update({
        attempts: FieldValue.increment(1)
      });

      const remainingAttempts = EMAIL_CONFIG.MAX_ATTEMPTS - (verificationData.attempts + 1);
      throw new HttpsError('invalid-argument',
        `Código incorrecto. Te quedan ${remainingAttempts} intentos`);
    }

    // Código correcto - marcar como verificado
    await db.collection('emailVerification').doc(userId).update({
      verified: true,
      verifiedAt: FieldValue.serverTimestamp()
    });

    // Actualizar el usuario en Firebase Auth
    try {
      await auth.updateUser(userId, {
        emailVerified: true
      });
      console.log(`✅ Email verificado en Auth para usuario ${userId}`);
    } catch (authError) {
      console.warn('No se pudo actualizar emailVerified en Auth:', authError);
    }

    console.log(`✅ Email verificado exitosamente para usuario ${userId}`);

    return {
      success: true,
      message: 'Email verificado exitosamente'
    };

  } catch (error) {
    if (error instanceof HttpsError) {
      throw error;
    }
    console.error('❌ Error verificando código:', error);
    throw new HttpsError('internal', 'Error interno del servidor');
  }
});

/**
 * Reenviar código de verificación
 */
exports.resendEmailVerificationCode = onCall(async (request) => {
  const {email, userId} = request.data;

  console.log(`🔄 Reenviando código para: ${email}`);

  if (!email || !userId) {
    throw new HttpsError('invalid-argument', 'Email y userId son requeridos');
  }

  try {

    // Verificar si existe un código anterior
    const existingDoc = await db.collection('emailVerification').doc(userId).get();

    if (existingDoc.exists) {
      const data = existingDoc.data();
      const now = new Date();
      const timeSinceCreated = now - data.createdAt.toDate();

      // Limitar reenvío a cada 60 segundos
      if (timeSinceCreated < 60000) {
        const waitTime = Math.ceil((60000 - timeSinceCreated) / 1000);
        throw new HttpsError('resource-exhausted',
          `Debes esperar ${waitTime} segundos antes de solicitar un nuevo código`);
      }
    }

    // Llamar a la función de envío
    return await exports.sendEmailVerificationCode.run(request);

  } catch (error) {
    if (error instanceof HttpsError) {
      throw error;
    }
    console.error('❌ Error reenviando código:', error);
    throw new HttpsError('internal', 'Error interno del servidor');
  }
});

/**
 * Limpiar códigos expirados (ejecuta cada hora)
 */
exports.cleanupExpiredCodes = onRequest({
  timeoutSeconds: 540,
  memory: '256MiB'
}, async (request, response) => {
  console.log('🧹 Iniciando limpieza de códigos expirados');

  try {
    const now = new Date();

    const expiredCodes = await db.collection('emailVerification')
      .where('expiresAt', '<', now)
      .get();

    const batch = db.batch();
    expiredCodes.forEach(doc => {
      batch.delete(doc.ref);
    });

    await batch.commit();

    console.log(`✅ Eliminados ${expiredCodes.size} códigos expirados`);
    response.json({
      success: true,
      deleted: expiredCodes.size,
      timestamp: new Date().toISOString()
    });

  } catch (error) {
    console.error('❌ Error limpiando códigos expirados:', error);
    response.status(500).json({
      error: 'Error interno',
      message: error.message
    });
  }
});

// ===================================================================
// SISTEMA DE RECUPERACIÓN DE CONTRASEÑA
// ===================================================================

/**
 * Enviar código de recuperación de contraseña
 * Valida que el email exista en Firebase Auth antes de enviar
 */
exports.sendPasswordResetCode = onCall(async (request) => {
  const {email} = request.data;

  console.log(`🔐 Iniciando recuperación de contraseña para: ${email}`);

  // Validaciones
  if (!email) {
    throw new HttpsError('invalid-argument', 'Email es requerido');
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    throw new HttpsError('invalid-argument', 'Email inválido');
  }

  try {
    const auth = getAuth();

    // Verificar que el email existe en Firebase Auth
    let userRecord;
    try {
      userRecord = await auth.getUserByEmail(email);
      console.log(`✅ Usuario encontrado: ${userRecord.uid}`);
    } catch (error) {
      // Por seguridad, no revelar si el email existe o no
      console.log(`⚠️ Email no encontrado: ${email}`);
      throw new HttpsError('not-found', 'Si el email existe, recibirás un código de recuperación');
    }

    const now = new Date();
    const expiresAt = new Date(now.getTime() + EMAIL_CONFIG.CODE_EXPIRY_MINUTES * 60 * 1000);

    // Generar código
    const resetCode = generateVerificationCode();
    console.log(`🔢 Código de reset generado: ${resetCode}`);

    // Obtener nombre del usuario para personalización
    let userName = 'Usuario';
    try {
      const userDoc = await db.collection('users').doc(userRecord.uid).get();
      if (userDoc.exists) {
        const userData = userDoc.data();
        userName = userData.nickname || userData.displayName || 'Usuario';
      }
    } catch (error) {
      console.warn('No se pudo obtener info del usuario:', error);
    }

    // Guardar código en colección separada 'passwordReset'
    await db.collection('passwordReset').doc(userRecord.uid).set({
      email: email,
      code: resetCode,
      createdAt: FieldValue.serverTimestamp(),
      expiresAt: expiresAt,
      verified: false,
      used: false,
      attempts: 0,
      userId: userRecord.uid
    });
    console.log(`💾 Código de reset guardado en Firestore`);

    // Enviar email con AWS SES
    if (transporter) {
      const mailOptions = {
        from: `"${EMAIL_CONFIG.FROM_NAME}" <${EMAIL_CONFIG.FROM_EMAIL}>`,
        to: email,
        subject: `${resetCode} - Recuperación de contraseña BiihLive`,
        html: getPasswordResetEmailTemplate(resetCode, userName)
      };

      await transporter.sendMail(mailOptions);
      console.log(`✅ Email de recuperación enviado a ${email}`);
    } else {
      console.error('❌ Transporter no configurado');
      throw new HttpsError('internal', 'Servicio de email no configurado');
    }

    return {
      success: true,
      message: 'Si el email existe, recibirás un código de recuperación',
      expiresAt: expiresAt.toISOString()
    };

  } catch (error) {
    console.error('❌ Error enviando código de recuperación:', error);
    if (error instanceof HttpsError) {
      throw error;
    }
    throw new HttpsError('internal', `Error interno: ${error.message}`);
  }
});

/**
 * Verificar código de recuperación de contraseña
 */
exports.verifyPasswordResetCode = onCall(async (request) => {
  const {email, code} = request.data;

  console.log(`🔍 Verificando código de reset para: ${email}`);

  // Validaciones
  if (!email || !code) {
    throw new HttpsError('invalid-argument', 'Email y código son requeridos');
  }

  if (code.length !== 6 || !/^\d{6}$/.test(code)) {
    throw new HttpsError('invalid-argument', 'El código debe ser de 6 dígitos');
  }

  try {
    const auth = getAuth();

    // Obtener usuario por email
    let userRecord;
    try {
      userRecord = await auth.getUserByEmail(email);
    } catch (error) {
      throw new HttpsError('not-found', 'Email no encontrado');
    }

    // Obtener datos de recuperación
    const resetDoc = await db.collection('passwordReset').doc(userRecord.uid).get();

    if (!resetDoc.exists) {
      throw new HttpsError('not-found', 'Código de recuperación no encontrado');
    }

    const resetData = resetDoc.data();
    const now = new Date();

    // Verificar expiración
    if (resetData.expiresAt.toDate() < now) {
      throw new HttpsError('deadline-exceeded', 'El código ha expirado');
    }

    // Verificar si ya fue usado
    if (resetData.used) {
      throw new HttpsError('failed-precondition', 'Este código ya fue usado');
    }

    // Verificar intentos máximos
    if (resetData.attempts >= EMAIL_CONFIG.MAX_ATTEMPTS) {
      throw new HttpsError('resource-exhausted', 'Demasiados intentos fallidos');
    }

    // Verificar si ya fue verificado
    if (resetData.verified) {
      // Si ya está verificado, permitir proceder al cambio de contraseña
      return {
        success: true,
        message: 'Código ya verificado, puedes cambiar tu contraseña',
        userId: userRecord.uid
      };
    }

    // Verificar el código
    if (resetData.code !== code) {
      // Incrementar intentos fallidos
      await db.collection('passwordReset').doc(userRecord.uid).update({
        attempts: FieldValue.increment(1)
      });

      const remainingAttempts = EMAIL_CONFIG.MAX_ATTEMPTS - (resetData.attempts + 1);
      throw new HttpsError('invalid-argument',
        `Código incorrecto. Te quedan ${remainingAttempts} intentos`);
    }

    // Código correcto - marcar como verificado
    await db.collection('passwordReset').doc(userRecord.uid).update({
      verified: true,
      verifiedAt: FieldValue.serverTimestamp()
    });

    console.log(`✅ Código de reset verificado para usuario ${userRecord.uid}`);

    return {
      success: true,
      message: 'Código verificado correctamente',
      userId: userRecord.uid
    };

  } catch (error) {
    if (error instanceof HttpsError) {
      throw error;
    }
    console.error('❌ Error verificando código de reset:', error);
    throw new HttpsError('internal', 'Error interno del servidor');
  }
});

/**
 * Cambiar contraseña con código verificado
 */
exports.resetPasswordWithCode = onCall(async (request) => {
  const {email, code, newPassword} = request.data;

  console.log(`🔑 Cambiando contraseña para: ${email}`);

  // Validaciones
  if (!email || !code || !newPassword) {
    throw new HttpsError('invalid-argument', 'Email, código y nueva contraseña son requeridos');
  }

  if (newPassword.length < 6) {
    throw new HttpsError('invalid-argument', 'La contraseña debe tener al menos 6 caracteres');
  }

  try {
    const auth = getAuth();

    // Obtener usuario por email
    let userRecord;
    try {
      userRecord = await auth.getUserByEmail(email);
    } catch (error) {
      throw new HttpsError('not-found', 'Email no encontrado');
    }

    // Verificar que el código fue verificado
    const resetDoc = await db.collection('passwordReset').doc(userRecord.uid).get();

    if (!resetDoc.exists) {
      throw new HttpsError('not-found', 'Código de recuperación no encontrado');
    }

    const resetData = resetDoc.data();
    const now = new Date();

    // Verificar expiración
    if (resetData.expiresAt.toDate() < now) {
      throw new HttpsError('deadline-exceeded', 'El código ha expirado');
    }

    // Verificar que fue verificado
    if (!resetData.verified) {
      throw new HttpsError('failed-precondition', 'Debes verificar el código primero');
    }

    // Verificar que no fue usado
    if (resetData.used) {
      throw new HttpsError('failed-precondition', 'Este código ya fue usado');
    }

    // Verificar que el código coincide
    if (resetData.code !== code) {
      throw new HttpsError('invalid-argument', 'Código incorrecto');
    }

    // Cambiar la contraseña en Firebase Auth
    await auth.updateUser(userRecord.uid, {
      password: newPassword
    });

    // Marcar el código como usado
    await db.collection('passwordReset').doc(userRecord.uid).update({
      used: true,
      usedAt: FieldValue.serverTimestamp()
    });

    console.log(`✅ Contraseña actualizada para usuario ${userRecord.uid}`);

    return {
      success: true,
      message: 'Contraseña actualizada exitosamente'
    };

  } catch (error) {
    if (error instanceof HttpsError) {
      throw error;
    }
    console.error('❌ Error cambiando contraseña:', error);
    throw new HttpsError('internal', 'Error interno del servidor');
  }
});

/**
 * Template HTML para email de recuperación de contraseña
 */
function getPasswordResetEmailTemplate(code, userName) {
  return `
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Recuperación de Contraseña - BiihLive</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            background-color: #f5f5f5;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            padding: 20px 16px;
        }
        .header {
            text-align: center;
            padding-bottom: 12px;
            border-bottom: 2px solid #2196F3;
            margin-bottom: 16px;
        }
        .logo {
            font-size: 24px;
            font-weight: bold;
            color: #2196F3;
            margin-bottom: 4px;
        }
        .title {
            font-size: 20px;
            color: #333333;
            margin: 12px 0 8px 0;
        }
        .code-container {
            background-color: #fff3cd;
            border: 2px solid #ffc107;
            border-radius: 8px;
            padding: 16px;
            text-align: center;
            margin: 12px 0;
        }
        .code {
            font-size: 32px;
            font-weight: bold;
            color: #856404;
            letter-spacing: 6px;
            margin: 8px 0;
        }
        .expiry {
            color: #666;
            font-size: 13px;
            margin-top: 6px;
        }
        .warning {
            background-color: #ffebee;
            border-left: 3px solid #f44336;
            border-radius: 4px;
            padding: 10px 12px;
            margin: 12px 0;
            color: #c62828;
            font-size: 13px;
            line-height: 1.4;
        }
        .footer {
            border-top: 1px solid #eeeeee;
            padding-top: 12px;
            margin-top: 16px;
            color: #999999;
            font-size: 12px;
            text-align: center;
            line-height: 1.3;
        }
        p {
            margin: 8px 0;
            color: #666666;
            font-size: 14px;
            line-height: 1.4;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="logo">🔐 BiihLive</div>
            <div style="color: #666; font-size: 14px;">Recuperación de Contraseña</div>
        </div>

        <h1 class="title">¡Hola ${userName || 'Usuario'}!</h1>

        <p>Recibimos una solicitud para recuperar tu contraseña. Usa este código:</p>

        <div class="code-container">
            <div class="code">${code}</div>
            <div class="expiry">Expira en ${EMAIL_CONFIG.CODE_EXPIRY_MINUTES} minutos</div>
        </div>

        <div class="warning">
            <strong>⚠️ Seguridad:</strong> Si NO solicitaste este cambio, ignora este email y tu contraseña permanecerá sin cambios.
        </div>

        <p style="font-size: 13px;">Ingresa este código en la app para crear tu nueva contraseña.</p>

        <div class="footer">
            Este email fue enviado por BiihLive<br>
            Si no solicitaste cambiar tu contraseña, ignora este mensaje
        </div>
    </div>
</body>
</html>
  `;
}

// ===================================================================
// OPTIMIZACIÓN DE IMÁGENES - RandomScore para Feed Aleatorio
// ===================================================================

/**
 * Cloud Function programada para agregar randomScore a posts existentes
 * Se ejecuta cada 24 horas hasta que todos los posts tengan randomScore
 * 
 * FASE 1 - PASO 1.2: Migración gradual de posts sin romper nada
 * 
 * Procesa 500 posts por ejecución para evitar timeouts
 * Una vez completado, se puede desactivar o eliminar
 */
exports.addRandomScoreToPosts = onSchedule({
  schedule: 'every 24 hours',
  timeZone: 'America/Argentina/Buenos_Aires',
  timeoutSeconds: 540,
  memory: '256MiB'
}, async (event) => {
  console.log('🔄 Iniciando migración de randomScore para posts...');
  
  try {
    const startTime = Date.now();
    
    // Obtener TODOS los posts (máximo 500 por ejecución)
    // No podemos buscar por campo inexistente en Firestore
    const postsSnapshot = await db.collection('posts')
      .limit(500)
      .get();
    
    if (postsSnapshot.empty) {
      console.log('📭 No hay posts en la colección');
      return {
        success: true,
        message: 'No hay posts para procesar',
        processed: 0,
        timestamp: new Date().toISOString()
      };
    }
    
    console.log(`📊 Analizando ${postsSnapshot.size} posts...`);
    
    // Filtrar posts que NO tienen randomScore
    const postsNeedingScore = postsSnapshot.docs.filter(doc => {
      const data = doc.data();
      return data.randomScore === undefined || data.randomScore === null;
    });
    
    if (postsNeedingScore.length === 0) {
      console.log('✅ Todos los posts ya tienen randomScore - migración completada');
      return {
        success: true,
        message: 'Migración completada - todos los posts tienen randomScore',
        processed: 0,
        timestamp: new Date().toISOString()
      };
    }
    
    console.log(`🔄 Procesando ${postsNeedingScore.length} posts sin randomScore`);
    
    // Batch update para mejor performance
    const batch = db.batch();
    let count = 0;
    
    postsNeedingScore.forEach(doc => {
      batch.update(doc.ref, {
        randomScore: Math.random(), // Valor aleatorio entre 0.0 y 1.0
        updatedAt: FieldValue.serverTimestamp()
      });
      count++;
    });
    
    // Commit del batch
    await batch.commit();
    
    const duration = Date.now() - startTime;
    console.log(`✅ ${count} posts actualizados con randomScore en ${duration}ms`);
    
    // Calcular posts restantes
    const remainingSnapshot = await db.collection('posts')
      .where('randomScore', '==', null)
      .count()
      .get();
    
    const remaining = remainingSnapshot.data().count;
    
    console.log(`📈 Progreso: ${count} procesados, ${remaining} restantes`);
    
    return {
      success: true,
      processed: count,
      remaining: remaining,
      duration: `${duration}ms`,
      timestamp: new Date().toISOString()
    };
    
  } catch (error) {
    console.error('❌ Error en addRandomScoreToPosts:', error);
    throw error;
  }
});

/**
 * Endpoint HTTP para ejecutar manualmente la migración (útil para testing)
 * 
 * Uso desde terminal:
 * curl -X POST https://us-central1-biihlive-aa5c3.cloudfunctions.net/migrateRandomScoreManual
 */
exports.migrateRandomScoreManual = onRequest({
  timeoutSeconds: 540,
  memory: '256MiB'
}, async (request, response) => {
  console.log('🔧 Ejecución manual de migración randomScore');
  
  try {
    const startTime = Date.now();
    
    // Obtener posts de tipo photo (limitado a 500)
    // No podemos usar where('randomScore', '==', null) porque el campo no existe
    const postsSnapshot = await db.collection('posts')
      .where('type', '==', 'photo')
      .limit(500)
      .get();
    
    if (postsSnapshot.empty) {
      response.json({
        success: true,
        message: '✅ No hay posts de tipo photo',
        processed: 0,
        timestamp: new Date().toISOString()
      });
      return;
    }
    
    // Filtrar en código los posts que NO tienen randomScore
    const postsToUpdate = postsSnapshot.docs.filter(doc => {
      const data = doc.data();
      return !data.hasOwnProperty('randomScore');
    });
    
    if (postsToUpdate.length === 0) {
      response.json({
        success: true,
        message: '✅ Todos los posts ya tienen randomScore',
        processed: 0,
        totalChecked: postsSnapshot.size,
        timestamp: new Date().toISOString()
      });
      return;
    }
    
    console.log(`📊 Procesando ${postsToUpdate.length} posts (de ${postsSnapshot.size} consultados)`);
    
    const batch = db.batch();
    let count = 0;
    
    postsToUpdate.forEach(doc => {
      batch.update(doc.ref, {
        randomScore: Math.random(),
        updatedAt: FieldValue.serverTimestamp()
      });
      count++;
    });
    
    await batch.commit();
    
    const duration = Date.now() - startTime;
    
    // Calcular posts restantes (aproximado)
    const allPostsSnapshot = await db.collection('posts')
      .where('type', '==', 'photo')
      .count()
      .get();
    
    const totalPosts = allPostsSnapshot.data().count;
    const estimatedRemaining = Math.max(0, totalPosts - count);
    
    console.log(`✅ Migración manual completada: ${count} posts`);
    
    response.json({
      success: true,
      processed: count,
      totalChecked: postsSnapshot.size,
      estimatedRemaining: estimatedRemaining,
      duration: `${duration}ms`,
      timestamp: new Date().toISOString()
    });
    
  } catch (error) {
    console.error('❌ Error en migración manual:', error);
    response.status(500).json({
      success: false,
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});
