import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {ControlsModule} from "../controls/controls-module";
import {ReactiveFormsModule} from "@angular/forms";
import {RdsInstancesHomeComponent} from "./rds-instances-home/rds-instances-home.component";
import {
  ProviderWizardStepsDirective,
  RdsInstancesCreateComponent,
  RdsInstancesWizardStep1Component,
  RdsInstancesWizardStep2Component
} from "./rds-instances-create/rds-instances-create.component";
import {RdsInstancesListItemComponent} from "./rds-instances-list-item.component";
import {RdsInstancesRoutingModule} from "./rds-instances-routing-module";
import {RdsInstancesComponent} from "./rds-instances.component";
import {RdsInstancesDetailsComponent} from "./rds-instances-details/rds-instances-details.component";

@NgModule({
  declarations: [
    RdsInstancesHomeComponent,
    RdsInstancesListItemComponent,
    RdsInstancesDetailsComponent,
    RdsInstancesCreateComponent,
    ProviderWizardStepsDirective,
    RdsInstancesWizardStep1Component,
    RdsInstancesWizardStep2Component,
    RdsInstancesListItemComponent,
    RdsInstancesComponent
  ],
  exports: [
    RdsInstancesListItemComponent
  ],
  imports: [
    CommonModule,
    ControlsModule,
    ReactiveFormsModule,
    RdsInstancesRoutingModule
  ]
})
export class RdsInstancesModule {
}
