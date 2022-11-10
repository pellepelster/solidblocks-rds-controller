import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {ControlsModule} from "../controls/controls-module";
import {ReactiveFormsModule} from "@angular/forms";
import {
  ProviderWizardStepsDirective,
  RdsInstancesCreateComponent,
  RdsInstancesWizardStep1Component,
  RdsInstancesWizardStep2Component
} from "./rds-instances-create/rds-instances-create.component";
import {RdsInstancesListItemComponent} from "./rds-instances-list-item.component";
import {RdsInstancesComponent} from "./rds-instances.component";
import {RdsInstancesHomeComponent} from "./rds-instances-home/rds-instances-home.component";
import {RouterModule} from "@angular/router";
import {RdsInstancesRoutingModule} from "./rds-instances-routing-module";

@NgModule({
  declarations: [
    RdsInstancesListItemComponent,
    RdsInstancesHomeComponent,
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
    RouterModule,
    RdsInstancesRoutingModule
  ]
})
export class RdsInstancesModule {
}
