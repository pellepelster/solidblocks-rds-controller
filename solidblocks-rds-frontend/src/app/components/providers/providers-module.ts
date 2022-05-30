import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {ControlsModule} from "../controls/controls-module";
import {ReactiveFormsModule} from "@angular/forms";
import {ProvidersHomeComponent} from "./providers-home/providers-home.component";
import {
  ProvidersWizardStep1Component,
  ProviderWizardStepsDirective,
  ProvidersCreateComponent, ProvidersWizardStep2Component
} from "./providers-create/providers-create.component";
import {ProvidersListItemComponent} from "./providers-list-item.component";
import {ProvidersRoutingModule} from "./providers-routing-module";
import {ProvidersComponent} from "./providers.component";
import {ProvidersDetailsComponent} from "./providers-details/providers-details.component";

@NgModule({
  declarations: [
    ProvidersHomeComponent,
    ProvidersDetailsComponent,
    ProvidersCreateComponent,
    ProviderWizardStepsDirective,
    ProvidersWizardStep1Component,
    ProvidersWizardStep2Component,
    ProvidersListItemComponent,
    ProvidersComponent
  ],
  exports: [],
  imports: [
    CommonModule,
    ControlsModule,
    ReactiveFormsModule,
    ProvidersRoutingModule
  ]
})
export class ProvidersModule {
}
