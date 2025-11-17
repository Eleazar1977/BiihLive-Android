// Firebase Cloud Functions para Biihlive - Password Recovery System
// Exportar todas las funciones de password recovery

const {
  sendPasswordRecoveryCode,
  verifyPasswordRecoveryCode,
  resetPasswordWithCode,
  resendPasswordRecoveryCode,
  cleanupExpiredTokens
} = require('./password-recovery-functions');

// Exportar funciones para deployment
exports.sendPasswordRecoveryCode = sendPasswordRecoveryCode;
exports.verifyPasswordRecoveryCode = verifyPasswordRecoveryCode;
exports.resetPasswordWithCode = resetPasswordWithCode;
exports.resendPasswordRecoveryCode = resendPasswordRecoveryCode;
exports.cleanupExpiredTokens = cleanupExpiredTokens;