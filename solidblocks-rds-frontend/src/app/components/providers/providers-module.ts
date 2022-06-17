import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {ControlsModule} from "../controls/controls-module";
import {ReactiveFormsModule} from "@angular/forms";
import {ProvidersHomeComponent} from "./providers-home/providers-home.component";
import {
  ProvidersCreateComponent,
  ProvidersWizardStep1Component,
  ProvidersWizardStep2Component,
  ProviderWizardStepsDirective
} from "./providers-create/providers-create.component";
import {ProvidersListItemComponent} from "./providers-list-item.component";
import {ProvidersRoutingModule} from "./providers-routing-module";
import {ProvidersComponent} from "./providers.component";
import {ProvidersDetailsComponent} from "./providers-details/providers-details.component";
import {RdsInstancesModule} from "../rds-instances/rds-instances-module";
import {ProviderStatusColorPipe} from "./providers-details/provider-status-color.pipe";

@NgModule({
  declarations: [
    ProvidersHomeComponent,
    ProvidersDetailsComponent,
    ProvidersCreateComponent,
    ProviderWizardStepsDirective,
    ProvidersWizardStep1Component,
    ProvidersWizardStep2Component,
    ProvidersListItemComponent,
    ProvidersComponent,
    ProviderStatusColorPipe
  ],
  exports: [],
  imports: [
    CommonModule,
    ControlsModule,
    ReactiveFormsModule,
    ProvidersRoutingModule,
    RdsInstancesModule,
  ]
})
export class ProvidersModule {
}
