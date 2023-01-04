import {Component, OnDestroy, OnInit} from '@angular/core';
import {ToastService} from "../../../utils/toast.service";
import {ProvidersService} from "../../../services/providers.service";
import {ProviderResponse} from "../../../services/types";
import {NavigationService} from "../../navigation/navigation.service";
import {map, Subscription, timer} from "rxjs";

@Component({
  selector: 'app-providers-list',
  templateUrl: './providers-list.component.html',
})
export class ProvidersListComponent implements OnInit, OnDestroy {

  providers: Array<ProviderResponse> = []

  timerSubscription: Subscription

  constructor(private navigationService: NavigationService, private providersService: ProvidersService, private toastsService: ToastService) {
  }

  ngOnInit(): void {
    this.providersService.list().subscribe(
      (response) => {
        this.providers = response.providers
      },
      (error) => {
        this.toastsService.handleErrorResponse(error)
      }
    )

    this.timerSubscription = timer(0, 5000).pipe(
      map(() => {
        this.updateProviders()
      })
    ).subscribe()
  }

  ngOnDestroy(): void {
    this.timerSubscription.unsubscribe()
  }

  private updateProviders(): void {
    this.providersService.list().subscribe(
      (response) => {
        this.providers = response.providers
      },
      (error) => {
        this.toastsService.handleErrorResponse(error)
      }
    )
  }

}
