import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {RdsInstanceResponseWrapper, RdsInstancesResponseWrapper} from "./types";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})

export class RdsInstancesService {

  constructor(private http: HttpClient) {
  }

  public create(name: String, provider: string) {
    return this.http.post<RdsInstanceResponseWrapper>(`${environment.apiAddress}/v1/rds-instances`, {
      name,
      provider
    });
  }

  public get(id: string) {
    return this.http.get<RdsInstanceResponseWrapper>(`${environment.apiAddress}/v1/rds-instances/${id}`);
  }

  public list() {
    return this.http.get<RdsInstancesResponseWrapper>(`${environment.apiAddress}/v1/rds-instances`);
  }

  public delete(id: string) {
    return this.http.delete(`${environment.apiAddress}/v1/rds-instances/${id}`);
  }

}
