/* eslint-disable */
// this is an auto generated file. This will be overwritten

export const createPerfilUsuario = /* GraphQL */ `
  mutation CreatePerfilUsuario(
    $input: CreatePerfilUsuarioInput!
    $condition: ModelPerfilUsuarioConditionInput
  ) {
    createPerfilUsuario(input: $input, condition: $condition) {
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
export const updatePerfilUsuario = /* GraphQL */ `
  mutation UpdatePerfilUsuario(
    $input: UpdatePerfilUsuarioInput!
    $condition: ModelPerfilUsuarioConditionInput
  ) {
    updatePerfilUsuario(input: $input, condition: $condition) {
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
export const deletePerfilUsuario = /* GraphQL */ `
  mutation DeletePerfilUsuario(
    $input: DeletePerfilUsuarioInput!
    $condition: ModelPerfilUsuarioConditionInput
  ) {
    deletePerfilUsuario(input: $input, condition: $condition) {
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
