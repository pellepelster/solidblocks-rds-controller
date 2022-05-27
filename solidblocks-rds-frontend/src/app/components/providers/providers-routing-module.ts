import {RouterModule, Routes} from "@angular/router";
import {NgModule} from "@angular/core";
import {ProvidersCreateComponent} from "./providers-create/providers-create.component";
import {ProvidersComponent} from "./providers.component";
import {ProvidersDetailsComponent} from "./providers-details/providers-details.component";
import {ProvidersHomeComponent} from "./providers-home/providers-home.component";

const routes: Routes = [
  {
    path: '',
    component: ProvidersComponent,

    children: [
      {
        path: '',
        component: ProvidersHomeComponent,
      },
      {
        path: 'create',
        component: ProvidersCreateComponent,
      },
      {
        path: ':id',
        component: ProvidersDetailsComponent,
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProvidersRoutingModule {
}
