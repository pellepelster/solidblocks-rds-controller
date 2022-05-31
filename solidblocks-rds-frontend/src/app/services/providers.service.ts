import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {ProviderResponseWrapper, ProvidersResponseWrapper} from "./types";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})

export class ProvidersService {

  constructor(private http: HttpClient) {
  }

  public create(name: String, apiKey: String) {
    return this.http.post<ProviderResponseWrapper>(`${environment.apiAddress}/v1/providers`, {
      name,
      apiKey
    });
  }

  public get(id: string) {
    return this.http.get<ProviderResponseWrapper>(`${environment.apiAddress}/v1/providers/${id}`);
  }

  public list() {
    return this.http.get<ProvidersResponseWrapper>(`${environment.apiAddress}/v1/providers`);
  }

  public delete(id: string) {
    return this.http.delete(`${environment.apiAddress}/v1/providers/${id}`);
  }

}
