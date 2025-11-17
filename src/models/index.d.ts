import { ModelInit, MutableModel, __modelMeta__, CustomIdentifier } from "@aws-amplify/datastore";
// @ts-ignore
import { LazyLoading, LazyLoadingDisabled } from "@aws-amplify/datastore";



type EagerUbicacion = {
  readonly ciudad?: string | null;
  readonly provincia?: string | null;
  readonly pais?: string | null;
}

type LazyUbicacion = {
  readonly ciudad?: string | null;
  readonly provincia?: string | null;
  readonly pais?: string | null;
}

export declare type Ubicacion = LazyLoading extends LazyLoadingDisabled ? EagerUbicacion : LazyUbicacion

export declare const Ubicacion: (new (init: ModelInit<Ubicacion>) => Ubicacion)

type EagerPerfilUsuario = {
  readonly [__modelMeta__]: {
    identifier: CustomIdentifier<PerfilUsuario, 'userId'>;
    readOnlyFields: 'createdAt' | 'updatedAt';
  };
  readonly userId: string;
  readonly nickname?: string | null;
  readonly fullName?: string | null;
  readonly description?: string | null;
  readonly totalScore?: number | null;
  readonly nivel?: number | null;
  readonly tipo?: string | null;
  readonly ubicacion?: Ubicacion | null;
  readonly rankingPreference?: string | null;
  readonly photoUrl?: string | null;
  readonly email?: string | null;
  readonly seguidores?: number | null;
  readonly siguiendo?: number | null;
  readonly createdAt?: string | null;
  readonly updatedAt?: string | null;
}

type LazyPerfilUsuario = {
  readonly [__modelMeta__]: {
    identifier: CustomIdentifier<PerfilUsuario, 'userId'>;
    readOnlyFields: 'createdAt' | 'updatedAt';
  };
  readonly userId: string;
  readonly nickname?: string | null;
  readonly fullName?: string | null;
  readonly description?: string | null;
  readonly totalScore?: number | null;
  readonly nivel?: number | null;
  readonly tipo?: string | null;
  readonly ubicacion?: Ubicacion | null;
  readonly rankingPreference?: string | null;
  readonly photoUrl?: string | null;
  readonly email?: string | null;
  readonly seguidores?: number | null;
  readonly siguiendo?: number | null;
  readonly createdAt?: string | null;
  readonly updatedAt?: string | null;
}

export declare type PerfilUsuario = LazyLoading extends LazyLoadingDisabled ? EagerPerfilUsuario : LazyPerfilUsuario

export declare const PerfilUsuario: (new (init: ModelInit<PerfilUsuario>) => PerfilUsuario) & {
  copyOf(source: PerfilUsuario, mutator: (draft: MutableModel<PerfilUsuario>) => MutableModel<PerfilUsuario> | void): PerfilUsuario;
}