import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {ControlsModule} from "../controls/controls-module";
import {ReactiveFormsModule} from "@angular/forms";
import {ProvidersListComponent} from "./providers-list/providers-list.component";
import {
  ProvidersCreateComponent,
  ProvidersWizardStep1Component,
  ProvidersWizardStep2Component,
  ProviderWizardStepsDirective
} from "./providers-create/providers-create.component";
import {ProvidersListItemComponent} from "./providers-list-item.component";
import {ProvidersRoutingModule} from "./providers-routing-module";
import {ProvidersComponent} from "./providers.component";
import {ProvidersHomeComponent} from "./providers-home/providers-home.component";
import {RdsInstancesModule} from "../rds-instances/rds-instances-module";
import {ProviderStatusColorPipe} from "./providers-home/provider-status-color.pipe";

@NgModule({
  declarations: [
    ProvidersListComponent,
    ProvidersHomeComponent,
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
