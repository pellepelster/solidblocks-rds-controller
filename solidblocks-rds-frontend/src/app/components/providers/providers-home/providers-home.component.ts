import {Component, OnInit} from '@angular/core';
import {ToastService} from "../../../utils/toast.service";
import {ProvidersService} from "../../../services/providers.service";
import {ProviderResponse} from "../../../services/types";
import {NavigationBreadcrumb, NavigationService} from "../../navigation/navigation.service";

@Component({
  selector: 'app-providers-home',
  templateUrl: './providers-home.component.html',
})
export class ProvidersHomeComponent implements OnInit {

  providers: Array<ProviderResponse>

  constructor(private navigationService: NavigationService, private providersService: ProvidersService, private toastsService: ToastService) {
  }

  ngOnInit(): void {
    this.navigationService.push(new NavigationBreadcrumb("Providers", "providers"))

    this.providersService.list().subscribe(
      (response) => {
        this.providers = response.providers
      },
      (error) => {
        this.toastsService.handleErrorResponse(error)
      },
    )
  }
}
