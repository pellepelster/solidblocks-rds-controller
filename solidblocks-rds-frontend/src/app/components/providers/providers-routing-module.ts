import {
  ActivatedRouteSnapshot,
  NavigationEnd,
  Params,
  Router,
  RouterModule,
  Routes
} from "@angular/router";
import {NgModule} from "@angular/core";
import {ProvidersCreateComponent} from "./providers-create/providers-create.component";
import {ProvidersComponent} from "./providers.component";
import {ProvidersDetailsComponent} from "./providers-details/providers-details.component";
import {ProvidersHomeComponent} from "./providers-home/providers-home.component";
import {BehaviorSubject} from "rxjs";
import {filter} from "rxjs/operators";
import {ContextService} from "../navigation/context.service";

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
        path: ':providerId',
        component: ProvidersDetailsComponent,
      },
      {
        path: ':providerId/rds-instances',
        loadChildren: () => import('../../components/rds-instances/rds-instances-module').then(m => m.RdsInstancesModule)
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

  constructor(private router: Router, private contextService: ContextService) {

    this.router = router;

    this.paramsSnapshot = {};
    this.params = new BehaviorSubject(this.paramsSnapshot);

    this.router.events
      .pipe(
        filter(
          (event: any): boolean => {
            return (event instanceof NavigationEnd)
          }
        )
      )
      .subscribe(
        (event: NavigationEnd): void => {
          const snapshot = this.router.routerState.snapshot.root;
          const nextParams = this.collectParams(snapshot);

          if (ProvidersRoutingModule.paramsAreDifferent(this.paramsSnapshot, nextParams)) {
            this.params.next(this.paramsSnapshot = nextParams);

            if (nextParams['providerId']) {
              contextService.nextProviderId(nextParams['providerId'])
            }

            if (nextParams['rdsInstanceId']) {
              contextService.nextRdsInstanceId(nextParams['rdsInstanceId'])
            }
          }
        }
      )
  }

  private collectParams(root: ActivatedRouteSnapshot): Params {
    const params: Params = {};

    (function mergeParamsFromSnapshot(snapshot: ActivatedRouteSnapshot) {
      Object.assign(params, snapshot.params);
      snapshot.children.forEach(mergeParamsFromSnapshot);

    })(root);
    return (params);
  }

  private static paramsAreDifferent(currentParams: Params, nextParams: Params): boolean {

    const currentKeys = Object.keys(currentParams);
    const nextKeys = Object.keys(nextParams);

    if (currentKeys.length !== nextKeys.length) {
      return true;
    }

    for (var i = 0, length = currentKeys.length; i < length; i++) {
      const key = currentKeys[i];

      if (currentParams[key] !== nextParams[key]) {
        return true;
      }
    }

    return false
  }
}
