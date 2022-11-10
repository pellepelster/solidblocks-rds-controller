import {Component, Input} from '@angular/core';
import {RdsInstanceResponse} from "../../services/types";

@Component({
  selector: 'app-rds-instances-list-item',
  template: `
    <div class="card" style="width: 18rem;">
      <div class="card-body">
        <h5 class="card-title">
          <a routerLink="rds/{{rdsInstance.id}}" routerLinkActive="active">RDS
            Instance {{rdsInstance.name}}</a>
        </h5>
        <p class="card-text">
          {{rdsInstance.id}}
        </p>
      </div>
    </div>
  `
})
export class RdsInstancesListItemComponent {

  @Input()
  rdsInstance: RdsInstanceResponse

}
