const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');
const nodemailer = require('nodemailer');

// Inicializar Firebase Admin
if (!admin.apps.length) {
    admin.initializeApp({
        projectId: 'biihlive-aa5c3'
    });
}

// 🔧 FIX: Configurar Firestore para usar SIEMPRE la base de datos "basebiihlive"
// Crear instancia única y consistente
const db = getFirestore('basebiihlive');

// 🔧 FIX: Wrapper para garantizar uso consistente de la instancia correcta
const getNamedFirestore = () => db;

// 🔧 FIX: Helper para FieldValue que use la instancia correcta
const FieldValue = admin.firestore.FieldValue;

// Configurar transporter de email (temporal - para testing sin credenciales)
let transporter;
try {
    const config = functions.config();
    const hasValidConfig = config.email?.user && config.email?.password &&
                          config.email.user !== 'noreply@biihlive.com' &&
                          config.email.password !== 'temp_password';

    if (hasValidConfig) {
        transporter = nodemailer.createTransporter({
            service: 'gmail',
            auth: {
                user: config.email.user,
                pass: config.email.password
            }
        });
        console.log('✅ Email transporter configurado con credenciales reales');
    } else {
        throw new Error('Credenciales de email no configuradas');
    }
} catch (error) {
    console.log('📧 Email config no disponible, usando modo temporal (no se envían emails reales)');
    // Transporter temporal que simula envío exitoso
    transporter = {
        sendMail: async (options) => {
            console.log(`📧 [TEMPORAL] Simulando envío a: ${options.to}`);
            console.log(`📧 [TEMPORAL] Asunto: ${options.subject}`);
            console.log(`📧 [TEMPORAL] El email se enviaría en producción con credenciales reales`);
            return { messageId: `temp-id-${Date.now()}` };
        }
    };
}

// Función para generar código de 6 dígitos
function generateVerificationCode() {
    return Math.floor(100000 + Math.random() * 900000).toString();
}

// Función para enviar código de recuperación
exports.sendPasswordRecoveryCode = functions.https.onCall(async (data, context) => {
    try {
        console.log(`📥 [DEBUG] Datos recibidos en sendPasswordRecoveryCode:`, JSON.stringify(data));
        const { email } = data;

        console.log(`📥 [DEBUG] Email extraído: "${email}", tipo: ${typeof email}`);

        // Validar email
        if (!email || typeof email !== 'string') {
            console.error(`❌ [DEBUG] Validación falló - email: ${email}, tipo: ${typeof email}`);
            throw new functions.https.HttpsError('invalid-argument', 'Email es requerido');
        }

        console.log(`Iniciando proceso de recuperación para email: ${email}`);

        // Verificar si el usuario existe en Firebase Auth
        let userRecord;
        try {
            userRecord = await admin.auth().getUserByEmail(email);
        } catch (error) {
            console.log(`Usuario no encontrado: ${email}`);
            throw new functions.https.HttpsError('not-found', 'No existe una cuenta con este email');
        }

        // Generar código de verificación
        const verificationCode = generateVerificationCode();
        const expirationTime = Date.now() + (10 * 60 * 1000); // 10 minutos

        // 🔧 FIX: Usar SIEMPRE la instancia de base de datos nombrada
        console.log(`📊 [DEBUG] Guardando token en base de datos "basebiihlive" para userId: ${userRecord.uid}`);

        await getNamedFirestore().collection('passwordRecoveryTokens').doc(userRecord.uid).set({
            email: email,
            code: verificationCode,
            createdAt: FieldValue.serverTimestamp(),
            expiresAt: new Date(expirationTime),
            attempts: 0,
            isUsed: false
        });

        console.log(`✅ [DEBUG] Token guardado exitosamente en "basebiihlive"`);

        // Enviar email con código
        const mailOptions = {
            from: '"Biihlive" <noreply@biihlive.com>',
            to: email,
            subject: 'Recupera tu acceso a Biihlive',
            html: `
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 20px; border-radius: 10px; }
                        .header { text-align: center; padding: 20px 0; }
                        .logo { color: #007BFF; font-size: 24px; font-weight: bold; }
                        .code { background-color: #007BFF; color: white; padding: 15px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 8px; border-radius: 8px; margin: 20px 0; }
                        .footer { font-size: 12px; color: #666; text-align: center; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Biihlive</div>
                            <h2>Recuperación de Contraseña</h2>
                        </div>

                        <p>Hola,</p>

                        <p>Has solicitado recuperar tu contraseña. Usa el siguiente código para continuar:</p>

                        <div class="code">${verificationCode}</div>

                        <p><strong>Este código expira en 10 minutos.</strong></p>

                        <p>Si no solicitaste esta recuperación, puedes ignorar este email.</p>

                        <div class="footer">
                            <p>© 2025 Biihlive. Todos los derechos reservados.</p>
                            <p>Este es un email automático, por favor no respondas.</p>
                        </div>
                    </div>
                </body>
                </html>
            `
        };

        await transporter.sendMail(mailOptions);

        console.log(`Código de recuperación enviado a: ${email}`);

        return {
            success: true,
            message: 'Código de recuperación enviado correctamente'
        };

    } catch (error) {
        console.error('Error en sendPasswordRecoveryCode:', error);

        if (error instanceof functions.https.HttpsError) {
            throw error;
        }

        throw new functions.https.HttpsError('internal', 'Error enviando código de recuperación');
    }
});

// Función para verificar código de recuperación
exports.verifyPasswordRecoveryCode = functions.https.onCall(async (data, context) => {
    try {
        const { email, code } = data;

        // Validar parámetros
        if (!email || !code) {
            throw new functions.https.HttpsError('invalid-argument', 'Email y código son requeridos');
        }

        if (code.length !== 6 || !/^\d+$/.test(code)) {
            throw new functions.https.HttpsError('invalid-argument', 'Código debe ser de 6 dígitos numéricos');
        }

        console.log(`Verificando código para email: ${email}`);

        // Obtener usuario de Firebase Auth
        let userRecord;
        try {
            userRecord = await admin.auth().getUserByEmail(email);
        } catch (error) {
            throw new functions.https.HttpsError('not-found', 'Usuario no encontrado');
        }

        // 🔧 FIX: Usar SIEMPRE la instancia nombrada
        const tokenDoc = await getNamedFirestore()
            .collection('passwordRecoveryTokens')
            .doc(userRecord.uid)
            .get();

        if (!tokenDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'Código de recuperación no encontrado');
        }

        const tokenData = tokenDoc.data();

        // Verificar si el código ya fue usado
        if (tokenData.isUsed) {
            throw new functions.https.HttpsError('invalid-argument', 'Código ya fue utilizado');
        }

        // Verificar expiración
        const now = new Date();
        if (now > tokenData.expiresAt.toDate()) {
            throw new functions.https.HttpsError('deadline-exceeded', 'El código ha expirado');
        }

        // Verificar número de intentos
        if (tokenData.attempts >= 5) {
            throw new functions.https.HttpsError('resource-exhausted', 'Demasiados intentos fallidos');
        }

        // Verificar código
        if (tokenData.code !== code) {
            // 🔧 FIX: Incrementar intentos usando instancia nombrada
            await getNamedFirestore()
                .collection('passwordRecoveryTokens')
                .doc(userRecord.uid)
                .update({
                    attempts: FieldValue.increment(1)
                });

            throw new functions.https.HttpsError('invalid-argument', 'Código incorrecto');
        }

        console.log(`Código verificado correctamente para: ${email}`);

        return {
            success: true,
            message: 'Código verificado correctamente'
        };

    } catch (error) {
        console.error('Error en verifyPasswordRecoveryCode:', error);

        if (error instanceof functions.https.HttpsError) {
            throw error;
        }

        throw new functions.https.HttpsError('internal', 'Error verificando código');
    }
});

// Función para cambiar contraseña con código verificado
exports.resetPasswordWithCode = functions.https.onCall(async (data, context) => {
    try {
        const { email, code, newPassword } = data;

        // Validar parámetros
        if (!email || !code || !newPassword) {
            throw new functions.https.HttpsError('invalid-argument', 'Todos los campos son requeridos');
        }

        if (newPassword.length < 6) {
            throw new functions.https.HttpsError('invalid-argument', 'La contraseña debe tener al menos 6 caracteres');
        }

        console.log(`Cambiando contraseña para email: ${email}`);

        // Obtener usuario de Firebase Auth
        let userRecord;
        try {
            userRecord = await admin.auth().getUserByEmail(email);
        } catch (error) {
            throw new functions.https.HttpsError('not-found', 'Usuario no encontrado');
        }

        // 🔧 FIX: Verificar token usando instancia nombrada
        const tokenDoc = await getNamedFirestore()
            .collection('passwordRecoveryTokens')
            .doc(userRecord.uid)
            .get();

        if (!tokenDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'Código expirado. Solicita uno nuevo');
        }

        const tokenData = tokenDoc.data();

        // Validaciones finales del token
        if (tokenData.isUsed) {
            throw new functions.https.HttpsError('invalid-argument', 'Código inválido o expirado');
        }

        if (new Date() > tokenData.expiresAt.toDate()) {
            throw new functions.https.HttpsError('deadline-exceeded', 'Código expirado. Solicita uno nuevo');
        }

        if (tokenData.code !== code) {
            throw new functions.https.HttpsError('invalid-argument', 'Código inválido');
        }

        // Cambiar contraseña en Firebase Auth
        await admin.auth().updateUser(userRecord.uid, {
            password: newPassword
        });

        // 🔧 FIX: Marcar token como usado en instancia nombrada
        await getNamedFirestore()
            .collection('passwordRecoveryTokens')
            .doc(userRecord.uid)
            .update({
                isUsed: true,
                usedAt: FieldValue.serverTimestamp()
            });

        // Opcional: Revocar todas las sesiones activas del usuario
        await admin.auth().revokeRefreshTokens(userRecord.uid);

        console.log(`Contraseña cambiada exitosamente para: ${email}`);

        return {
            success: true,
            message: 'Contraseña cambiada exitosamente'
        };

    } catch (error) {
        console.error('Error en resetPasswordWithCode:', error);

        if (error instanceof functions.https.HttpsError) {
            throw error;
        }

        throw new functions.https.HttpsError('internal', 'Error cambiando contraseña');
    }
});

// Función para reenviar código de recuperación
exports.resendPasswordRecoveryCode = functions.https.onCall(async (data, context) => {
    try {
        const { email } = data;

        // Validar email
        if (!email || typeof email !== 'string') {
            throw new functions.https.HttpsError('invalid-argument', 'Email es requerido');
        }

        console.log(`Reenviando código de recuperación para: ${email}`);

        // Verificar si el usuario existe
        let userRecord;
        try {
            userRecord = await admin.auth().getUserByEmail(email);
        } catch (error) {
            throw new functions.https.HttpsError('not-found', 'Usuario no encontrado');
        }

        // 🔧 FIX: Verificar límite usando instancia nombrada
        const tokenDoc = await getNamedFirestore()
            .collection('passwordRecoveryTokens')
            .doc(userRecord.uid)
            .get();

        if (tokenDoc.exists) {
            const tokenData = tokenDoc.data();
            const timeSinceCreated = Date.now() - tokenData.createdAt.toMillis();

            // Limitar reenvíos a uno por minuto
            if (timeSinceCreated < 60000) {
                throw new functions.https.HttpsError('resource-exhausted',
                    'Debes esperar antes de solicitar un nuevo código');
            }
        }

        // Generar nuevo código
        const verificationCode = generateVerificationCode();
        const expirationTime = Date.now() + (10 * 60 * 1000);

        // 🔧 FIX: Actualizar código usando instancia nombrada
        await getNamedFirestore().collection('passwordRecoveryTokens').doc(userRecord.uid).set({
            email: email,
            code: verificationCode,
            createdAt: FieldValue.serverTimestamp(),
            expiresAt: new Date(expirationTime),
            attempts: 0,
            isUsed: false
        });

        // Enviar email
        const mailOptions = {
            from: '"Biihlive" <noreply@biihlive.com>',
            to: email,
            subject: 'Nuevo código de recuperación - Biihlive',
            html: `
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 20px; border-radius: 10px; }
                        .header { text-align: center; padding: 20px 0; }
                        .logo { color: #007BFF; font-size: 24px; font-weight: bold; }
                        .code { background-color: #007BFF; color: white; padding: 15px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 8px; border-radius: 8px; margin: 20px 0; }
                        .footer { font-size: 12px; color: #666; text-align: center; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Biihlive</div>
                            <h2>Nuevo Código de Recuperación</h2>
                        </div>

                        <p>Has solicitado un nuevo código de recuperación:</p>

                        <div class="code">${verificationCode}</div>

                        <p><strong>Este código expira en 10 minutos.</strong></p>

                        <div class="footer">
                            <p>© 2025 Biihlive. Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
            `
        };

        await transporter.sendMail(mailOptions);

        console.log(`Nuevo código enviado a: ${email}`);

        return {
            success: true,
            message: 'Nuevo código enviado correctamente'
        };

    } catch (error) {
        console.error('Error en resendPasswordRecoveryCode:', error);

        if (error instanceof functions.https.HttpsError) {
            throw error;
        }

        throw new functions.https.HttpsError('internal', 'Error reenviando código');
    }
});

// ==============================================================================
// FUNCIONES DE VERIFICACIÓN DE EMAIL (PARA REGISTRO)
// ==============================================================================

// Función para enviar código de verificación de email
exports.sendEmailVerificationCode = functions.https.onCall(async (data, context) => {
    try {
        console.log('🔍 DEBUG - data.data recibido:', data.data);
        console.log('🔍 DEBUG - Extrayendo email y userId de data.data');

        const { email, userId } = data.data || data;

        console.log(`🔍 DEBUG - email extraído: '${email}', userId extraído: '${userId}'`);
        console.log(`🔍 DEBUG - email truthy: ${!!email}, userId truthy: ${!!userId}`);

        if (!email || !userId) {
            console.log('❌ ERROR - Email y/o userId están vacíos o undefined');
            throw new functions.https.HttpsError('invalid-argument', 'Email y userId son requeridos');
        }

        console.log(`Enviando código de verificación para registro: ${email}`);

        // Generar código de verificación
        const verificationCode = generateVerificationCode();
        const expirationTime = Date.now() + (10 * 60 * 1000); // 10 minutos

        // 🔧 FIX: Guardar código usando instancia nombrada
        console.log(`📊 [DEBUG] Guardando en "emailVerificationCodes" en base "basebiihlive"`);

        await getNamedFirestore().collection('emailVerificationCodes').doc(userId).set({
            email: email,
            code: verificationCode,
            createdAt: FieldValue.serverTimestamp(),
            expiresAt: new Date(expirationTime),
            attempts: 0,
            isUsed: false
        });

        console.log(`✅ [DEBUG] Código de verificación guardado exitosamente en "basebiihlive"`);

        // Enviar email con código
        const mailOptions = {
            from: '"Biihlive" <noreply@biihlive.com>',
            to: email,
            subject: 'Verifica tu email en Biihlive',
            html: `
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 20px; border-radius: 10px; }
                        .header { text-align: center; padding: 20px 0; }
                        .logo { color: #007BFF; font-size: 24px; font-weight: bold; }
                        .code { background-color: #007BFF; color: white; padding: 15px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 8px; border-radius: 8px; margin: 20px 0; }
                        .footer { font-size: 12px; color: #666; text-align: center; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Biihlive</div>
                            <h2>Verificación de Email</h2>
                        </div>

                        <p>¡Bienvenido a Biihlive!</p>

                        <p>Para completar tu registro, verifica tu email con el siguiente código:</p>

                        <div class="code">${verificationCode}</div>

                        <p><strong>Este código expira en 10 minutos.</strong></p>

                        <div class="footer">
                            <p>© 2025 Biihlive. Todos los derechos reservados.</p>
                            <p>Este es un email automático, por favor no respondas.</p>
                        </div>
                    </div>
                </body>
                </html>
            `
        };

        await transporter.sendMail(mailOptions);

        console.log(`Código de verificación enviado a: ${email}`);

        return {
            success: true,
            message: 'Código de verificación enviado'
        };

    } catch (error) {
        console.error('Error en sendEmailVerificationCode:', error);
        throw new functions.https.HttpsError('internal', 'Error enviando código');
    }
});

// Función para verificar código de email
exports.verifyEmailCode = functions.https.onCall(async (data, context) => {
    try {
        const { userId, code } = data;

        if (!userId || !code) {
            throw new functions.https.HttpsError('invalid-argument', 'UserId y código son requeridos');
        }

        if (code.length !== 6 || !/^\d+$/.test(code)) {
            throw new functions.https.HttpsError('invalid-argument', 'Código debe ser de 6 dígitos numéricos');
        }

        console.log(`Verificando código de email para usuario: ${userId}`);

        // 🔧 FIX: Usar instancia nombrada
        const tokenDoc = await getNamedFirestore()
            .collection('emailVerificationCodes')
            .doc(userId)
            .get();

        if (!tokenDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'Código de verificación no encontrado');
        }

        const tokenData = tokenDoc.data();

        // Verificar si el código ya fue usado
        if (tokenData.isUsed) {
            throw new functions.https.HttpsError('invalid-argument', 'Código ya fue utilizado');
        }

        // Verificar expiración
        const now = new Date();
        if (now > tokenData.expiresAt.toDate()) {
            throw new functions.https.HttpsError('deadline-exceeded', 'El código ha expirado');
        }

        // Verificar número de intentos
        if (tokenData.attempts >= 5) {
            throw new functions.https.HttpsError('resource-exhausted', 'Demasiados intentos fallidos');
        }

        // Verificar código
        if (tokenData.code !== code) {
            // 🔧 FIX: Incrementar intentos usando instancia nombrada
            await getNamedFirestore()
                .collection('emailVerificationCodes')
                .doc(userId)
                .update({
                    attempts: FieldValue.increment(1)
                });

            throw new functions.https.HttpsError('invalid-argument', 'Código incorrecto');
        }

        // 🔧 FIX: Marcar como usado usando instancia nombrada
        await getNamedFirestore()
            .collection('emailVerificationCodes')
            .doc(userId)
            .update({
                isUsed: true,
                verifiedAt: FieldValue.serverTimestamp()
            });

        console.log(`Código verificado correctamente para usuario: ${userId}`);

        return {
            success: true,
            message: 'Email verificado exitosamente'
        };

    } catch (error) {
        console.error('Error en verifyEmailCode:', error);

        if (error instanceof functions.https.HttpsError) {
            throw error;
        }

        throw new functions.https.HttpsError('internal', 'Error verificando código');
    }
});

// Función para reenviar código de verificación de email
exports.resendEmailVerificationCode = functions.https.onCall(async (data, context) => {
    try {
        const { email, userId } = data;

        if (!email || !userId) {
            throw new functions.https.HttpsError('invalid-argument', 'Email y userId son requeridos');
        }

        console.log(`Reenviando código de verificación a: ${email}`);

        // 🔧 FIX: Verificar límite usando instancia nombrada
        const tokenDoc = await getNamedFirestore()
            .collection('emailVerificationCodes')
            .doc(userId)
            .get();

        if (tokenDoc.exists) {
            const tokenData = tokenDoc.data();
            const timeSinceCreated = Date.now() - tokenData.createdAt.toMillis();

            // Limitar reenvíos a uno por minuto
            if (timeSinceCreated < 60000) {
                throw new functions.https.HttpsError('resource-exhausted',
                    'Debes esperar antes de solicitar un nuevo código');
            }
        }

        // Generar nuevo código
        const verificationCode = generateVerificationCode();
        const expirationTime = Date.now() + (10 * 60 * 1000);

        // 🔧 FIX: Actualizar código usando instancia nombrada
        await getNamedFirestore().collection('emailVerificationCodes').doc(userId).set({
            email: email,
            code: verificationCode,
            createdAt: FieldValue.serverTimestamp(),
            expiresAt: new Date(expirationTime),
            attempts: 0,
            isUsed: false
        });

        // Enviar email
        const mailOptions = {
            from: '"Biihlive" <noreply@biihlive.com>',
            to: email,
            subject: 'Nuevo código de verificación - Biihlive',
            html: `
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 20px; border-radius: 10px; }
                        .header { text-align: center; padding: 20px 0; }
                        .logo { color: #007BFF; font-size: 24px; font-weight: bold; }
                        .code { background-color: #007BFF; color: white; padding: 15px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 8px; border-radius: 8px; margin: 20px 0; }
                        .footer { font-size: 12px; color: #666; text-align: center; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Biihlive</div>
                            <h2>Nuevo Código de Verificación</h2>
                        </div>

                        <p>Has solicitado un nuevo código de verificación:</p>

                        <div class="code">${verificationCode}</div>

                        <p><strong>Este código expira en 10 minutos.</strong></p>

                        <div class="footer">
                            <p>© 2025 Biihlive. Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
            `
        };

        await transporter.sendMail(mailOptions);

        console.log(`Nuevo código enviado a: ${email}`);

        return {
            success: true,
            message: 'Nuevo código enviado'
        };

    } catch (error) {
        console.error('Error en resendEmailVerificationCode:', error);

        if (error instanceof functions.https.HttpsError) {
            throw error;
        }

        throw new functions.https.HttpsError('internal', 'Error reenviando código');
    }
});

// Función para limpiar tokens de verificación expirados
exports.cleanupExpiredCodes = functions.scheduler.onSchedule('every 1 hours', async (event) => {
    try {
        const now = new Date();

        // 🔧 FIX: Limpiar tokens usando instancia nombrada
        const expiredEmailTokens = await getNamedFirestore()
            .collection('emailVerificationCodes')
            .where('expiresAt', '<', now)
            .get();

        const batch = getNamedFirestore().batch();
        expiredEmailTokens.forEach((doc) => {
            batch.delete(doc.ref);
        });

        await batch.commit();

        console.log(`Eliminados ${expiredEmailTokens.size} tokens de verificación expirados`);

        return {
            success: true,
            deletedEmailTokens: expiredEmailTokens.size
        };

    } catch (error) {
        console.error('Error limpiando tokens de verificación expirados:', error);
        throw error;
    }
});

// ==============================================================================
// FUNCIONES DE PASSWORD RECOVERY
// ==============================================================================

// Función para limpiar tokens expirados (ejecutar periódicamente)
exports.cleanupExpiredTokens = functions.scheduler.onSchedule('every 1 hours', async (event) => {
    try {
        const now = new Date();

        // 🔧 FIX: Obtener tokens expirados usando instancia nombrada
        const expiredTokens = await getNamedFirestore()
            .collection('passwordRecoveryTokens')
            .where('expiresAt', '<', now)
            .get();

        // 🔧 FIX: Eliminar tokens usando instancia nombrada
        const batch = getNamedFirestore().batch();
        expiredTokens.forEach((doc) => {
            batch.delete(doc.ref);
        });

        await batch.commit();

        console.log(`Eliminados ${expiredTokens.size} tokens expirados`);

        return {
            success: true,
            deletedCount: expiredTokens.size
        };

    } catch (error) {
        console.error('Error limpiando tokens expirados:', error);
        throw error;
    }
});