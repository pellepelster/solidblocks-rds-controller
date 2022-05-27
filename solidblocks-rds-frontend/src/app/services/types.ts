export interface User {
  email: string
}

export interface ConfigValueDefinition {
  name: string
  type: string
}

// provider
export interface ProviderResponse {
  name: string
  id: string
}

export interface ProvidersResponseWrapper {
  providers: Array<ProviderResponse>
}

export interface ProviderResponseWrapper {
  provider: ProviderResponse
}

export interface CreateProviderResponse {
  provider: ProviderResponse
  messages: Array<MessageResponse>
}

// service
export interface ServiceCatalogItem {
  type: string
  description: string
}

export interface ServiceCatalogResponse {
  items: Array<ServiceCatalogItem>
}

export interface Service {
  id: string
  name: string
}


export interface MessageResponse {
  attribute: string
  code: string
}

