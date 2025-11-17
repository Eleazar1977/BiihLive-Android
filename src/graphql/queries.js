/* eslint-disable */
// this is an auto generated file. This will be overwritten

export const getPerfilUsuario = /* GraphQL */ `
  query GetPerfilUsuario($userId: ID!) {
    getPerfilUsuario(userId: $userId) {
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
export const listPerfilUsuarios = /* GraphQL */ `
  query ListPerfilUsuarios(
    $userId: ID
    $filter: ModelPerfilUsuarioFilterInput
    $limit: Int
    $nextToken: String
    $sortDirection: ModelSortDirection
  ) {
    listPerfilUsuarios(
      userId: $userId
      filter: $filter
      limit: $limit
      nextToken: $nextToken
      sortDirection: $sortDirection
    ) {
      items {
        userId
        nickname
        fullName
        description
        totalScore
        nivel
        tipo
        rankingPreference
        photoUrl
        email
        seguidores
        siguiendo
        createdAt
        updatedAt
        __typename
      }
      nextToken
      __typename
    }
  }
`;
