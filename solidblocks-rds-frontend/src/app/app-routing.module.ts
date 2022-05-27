import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {ProvidersHomeComponent} from "./components/providers/providers-home/providers-home.component";

const routes: Routes = [
  {
    path: '',
    component: ProvidersHomeComponent
  },
  {
    path: 'services',
    loadChildren: () => import('./components/services/services-module').then(m => m.ServicesModule)
  },
  {
    path: 'providers',
    loadChildren: () => import('./components/providers/providers-module').then(m => m.ProvidersModule)
  },

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
