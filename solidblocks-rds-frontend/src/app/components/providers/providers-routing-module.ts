import {Params, RouterModule, Routes} from "@angular/router";
import {NgModule} from "@angular/core";
import {ProvidersCreateComponent} from "./providers-create/providers-create.component";
import {ProvidersComponent} from "./providers.component";
import {ProvidersHomeComponent} from "./providers-home/providers-home.component";
import {ProvidersListComponent} from "./providers-list/providers-list.component";
import {BehaviorSubject} from "rxjs";

const routes: Routes = [
  {
    path: '',
    component: ProvidersComponent,

    children: [
      {
        path: '',
        component: ProvidersListComponent
      },
      {
        path: 'create',
        component: ProvidersCreateComponent,
      },
      {
        path: ':providerId/rds',
        loadChildren: () => import('../rds-instances/rds-instances-module').then(m => m.RdsInstancesModule)
      },
      {
        path: ':providerId',
        component: ProvidersHomeComponent,
      },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProvidersRoutingModule {

  public params: BehaviorSubject<Params>;
  public paramsSnapshot: Params;

  constructor() {
    this.paramsSnapshot = {};
    this.params = new BehaviorSubject(this.paramsSnapshot);
  }

}
