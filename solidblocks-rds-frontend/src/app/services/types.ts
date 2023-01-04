export interface User {
  email: string
}

export interface ConfigValueDefinition {
  name: string
  type: string
}

export interface StatusResponse {
  health: string
}

// provider
export interface ProviderResponse {
  name: string
  id: string
  status: StatusResponse
}

export interface ProvidersResponseWrapper {
  providers: Array<ProviderResponse>
}

export interface ProviderResponseWrapper {
  provider: ProviderResponse
}

export interface ProviderStatusResponse {
  id: string
  status: string
}

export interface MessageResponse {
  attribute: string
  code: string
}

// rds instance
export interface RdsInstanceResponse {
  name: string
  id: string
  status: StatusResponse
}

export interface RdsInstancesResponseWrapper {
  rdsInstances: Array<RdsInstanceResponse>
}

export interface RdsInstanceResponseWrapper {
  rdsInstance: RdsInstanceResponse
}
