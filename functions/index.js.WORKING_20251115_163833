const {onCall, onRequest, HttpsError} = require('firebase-functions/v2/https');
const {initializeApp} = require('firebase-admin/app');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const {getAuth} = require('firebase-admin/auth');
const nodemailer = require('nodemailer');

initializeApp();

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
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            padding: 40px 20px;
        }
        .header {
            text-align: center;
            padding-bottom: 30px;
            border-bottom: 2px solid #2196F3;
        }
        .logo {
            font-size: 28px;
            font-weight: bold;
            color: #2196F3;
            margin-bottom: 10px;
        }
        .title {
            font-size: 24px;
            color: #333333;
            margin: 30px 0;
        }
        .code-container {
            background-color: #f8f9fa;
            border: 2px solid #2196F3;
            border-radius: 10px;
            padding: 30px;
            text-align: center;
            margin: 30px 0;
        }
        .code {
            font-size: 36px;
            font-weight: bold;
            color: #2196F3;
            letter-spacing: 8px;
            margin: 15px 0;
        }
        .instructions {
            color: #666666;
            line-height: 1.6;
            margin: 20px 0;
        }
        .footer {
            border-top: 1px solid #eeeeee;
            padding-top: 20px;
            margin-top: 30px;
            color: #999999;
            font-size: 14px;
            text-align: center;
        }
        .warning {
            background-color: #fff3cd;
            border: 1px solid #ffeaa7;
            border-radius: 5px;
            padding: 15px;
            margin: 20px 0;
            color: #856404;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="logo">📱 BiihLive</div>
            <div style="color: #666; font-size: 16px;">Verificación de Email</div>
        </div>

        <h1 class="title">¡Hola ${userName || 'Usuario'}!</h1>

        <p class="instructions">
            Hemos recibido una solicitud para verificar tu cuenta de BiihLive.
            Usa el siguiente código de verificación para completar el proceso:
        </p>

        <div class="code-container">
            <div style="color: #666; margin-bottom: 10px;">Tu código de verificación es:</div>
            <div class="code">${code}</div>
            <div style="color: #666; font-size: 14px; margin-top: 10px;">
                Este código expirará en ${EMAIL_CONFIG.CODE_EXPIRY_MINUTES} minutos
            </div>
        </div>

        <div class="warning">
            <strong>⚠️ Importante:</strong><br>
            • Este código es válido solo por ${EMAIL_CONFIG.CODE_EXPIRY_MINUTES} minutos<br>
            • No compartas este código con nadie<br>
            • Si no solicitaste este código, puedes ignorar este email
        </div>

        <p class="instructions">
            Ingresa este código en la aplicación BiihLive para verificar tu cuenta.
            Si tienes problemas, contacta con nuestro soporte.
        </p>

        <div class="footer">
            <p>Este email fue enviado por BiihLive</p>
            <p>Si no solicitaste esta verificación, puedes ignorar este mensaje.</p>
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
    const db = getFirestore();
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
    const db = getFirestore();
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
    const db = getFirestore();

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
    const db = getFirestore();
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
