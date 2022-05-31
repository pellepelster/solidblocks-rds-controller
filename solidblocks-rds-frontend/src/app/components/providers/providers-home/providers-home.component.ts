import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {ToastService} from "../../../utils/toast.service";
import {ProvidersService} from "../../../services/providers.service";
import {ProviderResponse} from "../../../services/types";

@Component({
  selector: 'app-providers-home',
  templateUrl: './providers-home.component.html',
})
export class ProvidersHomeComponent implements OnInit, OnDestroy {

  providers: Array<ProviderResponse>

  constructor(private route: ActivatedRoute, private providersService: ProvidersService, private toastsService: ToastService) {
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

  ngOnDestroy() {
  }

}
