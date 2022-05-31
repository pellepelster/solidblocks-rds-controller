import {Injectable} from '@angular/core';
import {BehaviorSubject, distinctUntilChanged} from "rxjs";
import {ProviderResponse, RdsInstanceResponse} from "../../services/types";
import {ProvidersService} from "../../services/providers.service";
import {filter} from "rxjs/operators";
import {RdsInstancesService} from "../../services/rds-instances.service";

@Injectable({
  providedIn: 'root',
})
export class ContextService {

  readonly currentProvider = new BehaviorSubject<ProviderResponse | null>(null)

  readonly currentProviderId = new BehaviorSubject<string>("");

  readonly currentRdsInstance = new BehaviorSubject<RdsInstanceResponse | null>(null)

  readonly currentRdsInstanceId = new BehaviorSubject<string>("");

  constructor(private providersService: ProvidersService, private rdsInstancesService: RdsInstancesService) {

    this.currentProviderId.pipe(filter(v => v !== ""), distinctUntilChanged()).subscribe(providerId => {
      this.providersService.get(providerId).subscribe(
        (next) => {
          this.nextProvider(next.provider)
        })
    })

    this.currentRdsInstanceId.pipe(filter(v => v !== ""), distinctUntilChanged()).subscribe(rdsInstanceId => {
      this.rdsInstancesService.get(rdsInstanceId).subscribe(
        (next) => {
          this.nextRdsInstance(next.rdsInstance)
        })
    })
  }

  nextProvider(provider: ProviderResponse) {
    this.currentProvider.next(provider)
  }

  nextProviderId(id: string) {
    this.currentProviderId.next(id)
  }

  nextRdsInstance(rdsInstance: RdsInstanceResponse) {
    this.currentRdsInstance.next(rdsInstance)
  }

  nextRdsInstanceId(id: string) {
    this.currentRdsInstanceId.next(id)
  }

}
