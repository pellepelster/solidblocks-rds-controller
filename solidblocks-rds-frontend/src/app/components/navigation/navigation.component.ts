import {Component, OnInit} from '@angular/core';
import {NavigationBreadcrumb, NavigationService} from "./navigation.service";
import {ProviderResponse} from "../../services/types";
import {ProvidersService} from "../../services/providers.service";


@Component({
  selector: 'app-navigation',
  templateUrl: './navigation.component.html',
})
export class NavigationComponent implements OnInit {

  breadcrumbs: NavigationBreadcrumb[] = []

  currentProvider: ProviderResponse;

  providers: ProviderResponse[];

  constructor(public navigationService: NavigationService, private providersService: ProvidersService) {
  }

  ngOnInit(): void {
    this.navigationService.breadcrumbs.subscribe((breadcrumbs) => {
      this.breadcrumbs = breadcrumbs as NavigationBreadcrumb[]
    })

    this.navigationService.currentProvider.subscribe((provider) => {
      this.currentProvider = provider as ProviderResponse
    })

    this.providersService.list().subscribe((providers) => {
      this.providers = providers.providers as ProviderResponse[]
    })
  }

}
