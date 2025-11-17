// @ts-check
import { initSchema } from '@aws-amplify/datastore';
import { schema } from './schema';



const { PerfilUsuario, Ubicacion } = initSchema(schema);

export {
  PerfilUsuario,
  Ubicacion
};