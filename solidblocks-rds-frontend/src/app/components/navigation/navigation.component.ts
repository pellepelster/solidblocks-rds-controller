import {Component, OnInit} from '@angular/core';
import {ProviderResponse} from "../../services/types";
import {ProvidersService} from "../../services/providers.service";
import {NavigationService} from "./navigation.service";


@Component({
  selector: 'app-navigation',
  templateUrl: './navigation.component.html',
})
export class NavigationComponent implements OnInit {

  currentProvider: ProviderResponse | null;

  providers: ProviderResponse[];

  constructor(public navigationService: NavigationService, private providersService: ProvidersService) {
  }

  ngOnInit(): void {

    this.navigationService.currentProvider.subscribe((provider) => {
      this.currentProvider = provider as ProviderResponse | null
    })

    this.providersService.list().subscribe((providers) => {
      this.providers = providers.providers as ProviderResponse[]
    })
  }

}
