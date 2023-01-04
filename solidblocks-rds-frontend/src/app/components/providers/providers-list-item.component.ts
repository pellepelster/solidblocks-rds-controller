import {Component, Input} from '@angular/core';
import {ProviderResponse} from "../../services/types";

@Component({
  selector: 'app-providers-list-item',
  template: `
    <div class="card" style="width: 20rem;">
      <div class="card-body">

        <div class="d-flex justify-content-between align-items-center">

          <h5 class="card-title">
            <a [routerLink]="[provider.id]">Provider {{provider.name}}</a>
          </h5>

          <app-status [status]="provider.status"></app-status>
        </div>

        <p class="card-text">
          Some information about the provider.
        </p>
      </div>
    </div>
  `
})
export class ProvidersListItemComponent {

  @Input()
  provider: ProviderResponse

}
