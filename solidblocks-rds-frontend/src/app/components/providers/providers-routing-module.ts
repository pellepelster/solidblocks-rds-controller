import {Params, Router, RouterModule, Routes} from "@angular/router";
import {NgModule} from "@angular/core";
import {ProvidersCreateComponent} from "./providers-create/providers-create.component";
import {ProvidersComponent} from "./providers.component";
import {ProvidersDetailsComponent} from "./providers-details/providers-details.component";
import {ProvidersHomeComponent} from "./providers-home/providers-home.component";
import {NavigationService} from "../navigation/navigation.service";
import {BehaviorSubject} from "rxjs";

const routes: Routes = [
  {
    path: '',
    component: ProvidersComponent,
    children: [
      {
        path: 'create',
        outlet: 'providers',
        component: ProvidersCreateComponent,
      },
      {
        path: ':providerId',
        outlet: 'providers',
        component: ProvidersDetailsComponent,
      },
      {
        path: '',
        pathMatch: 'full',
        outlet: 'providers',
        component: ProvidersHomeComponent
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

  constructor(private router: Router, private navigationService: NavigationService) {

    this.router = router;

    this.paramsSnapshot = {};
    this.params = new BehaviorSubject(this.paramsSnapshot);
  }

}
