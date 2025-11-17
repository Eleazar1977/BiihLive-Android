/* eslint-disable */
// this is an auto generated file. This will be overwritten

export const onCreatePerfilUsuario = /* GraphQL */ `
  subscription OnCreatePerfilUsuario(
    $filter: ModelSubscriptionPerfilUsuarioFilterInput
  ) {
    onCreatePerfilUsuario(filter: $filter) {
      userId
      nickname
      fullName
      description
      totalScore
      nivel
      tipo
      ubicacion {
        ciudad
        provincia
        pais
        __typename
      }
      rankingPreference
      photoUrl
      email
      seguidores
      siguiendo
      createdAt
      updatedAt
      __typename
    }
  }
`;
export const onUpdatePerfilUsuario = /* GraphQL */ `
  subscription OnUpdatePerfilUsuario(
    $filter: ModelSubscriptionPerfilUsuarioFilterInput
  ) {
    onUpdatePerfilUsuario(filter: $filter) {
      userId
      nickname
      fullName
      description
      totalScore
      nivel
      tipo
      ubicacion {
        ciudad
        provincia
        pais
        __typename
      }
      rankingPreference
      photoUrl
      email
      seguidores
      siguiendo
      createdAt
      updatedAt
      __typename
    }
  }
`;
export const onDeletePerfilUsuario = /* GraphQL */ `
  subscription OnDeletePerfilUsuario(
    $filter: ModelSubscriptionPerfilUsuarioFilterInput
  ) {
    onDeletePerfilUsuario(filter: $filter) {
      userId
      nickname
      fullName
      description
      totalScore
      nivel
      tipo
      ubicacion {
        ciudad
        provincia
        pais
        __typename
      }
      rankingPreference
      photoUrl
      email
      seguidores
      siguiendo
      createdAt
      updatedAt
      __typename
    }
  }
`;
