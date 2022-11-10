import {Component, OnInit} from '@angular/core';
import {ToastService} from "../../../utils/toast.service";
import {ProvidersService} from "../../../services/providers.service";
import {ProviderResponse} from "../../../services/types";
import {NavigationService} from "../../navigation/navigation.service";

@Component({
  selector: 'app-providers-list',
  templateUrl: './providers-list.component.html',
})
export class ProvidersListComponent implements OnInit {

  providers: Array<ProviderResponse>

  constructor(private navigationService: NavigationService, private providersService: ProvidersService, private toastsService: ToastService) {
  }

  ngOnInit(): void {
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
