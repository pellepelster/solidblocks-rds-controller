import {RouterModule, Routes} from "@angular/router";
import {NgModule} from "@angular/core";
import {RdsInstancesCreateComponent} from "./rds-instances-create/rds-instances-create.component";
import {RdsInstancesComponent} from "./rds-instances.component";
import {RdsInstancesDetailsComponent} from "./rds-instances-details/rds-instances-details.component";
import {RdsInstancesHomeComponent} from "./rds-instances-home/rds-instances-home.component";

const routes: Routes = [
  {
    path: '',
    component: RdsInstancesComponent,

    children: [
      {
        path: '',
        component: RdsInstancesHomeComponent,
      },
      {
        path: 'create',
        component: RdsInstancesCreateComponent,
      },
      {
        path: ':rdsInstanceId',
        component: RdsInstancesDetailsComponent,
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class RdsInstancesRoutingModule {
}
