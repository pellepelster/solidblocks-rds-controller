import {Component, Input} from '@angular/core';
import {ProviderResponse} from "../../services/types";

@Component({
  selector: 'app-providers-list-item',
  template: `
    <div class="card" style="width: 18rem;">
      <div class="card-body">
        <h5 class="card-title">
          <a routerLink="/providers/{{provider.id}}" routerLinkActive="active">Provider {{provider.name}}</a>
        </h5>
        <p class="card-text">
          {{provider.id}}
        </p>
      </div>
    </div>
  `
})
export class ProvidersListItemComponent {

  @Input()
  provider: ProviderResponse

}
