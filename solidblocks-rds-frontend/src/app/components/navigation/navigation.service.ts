import {Injectable} from '@angular/core';
import {Subject} from "rxjs";
import {ProviderResponse} from "../../services/types";


export class NavigationBreadcrumb {
  constructor(public name: string, public path: string) {
  }
}

@Injectable({
  providedIn: 'root',
})
export class NavigationService {

  public breadcrumbs = new Subject<NavigationBreadcrumb[]>();

  public currentProvider = new Subject<ProviderResponse>();

  private breadcrumbsList: NavigationBreadcrumb[] = []

  selectProvider(provider: ProviderResponse) {
    this.currentProvider.next(provider)
  }

  push(breadcrumb: NavigationBreadcrumb) {

    const index = this.breadcrumbsList.findIndex((b) => b.path == breadcrumb.path)

    if (index < 0) {
      this.breadcrumbsList.push(breadcrumb)
    } else {
      this.breadcrumbsList = this.breadcrumbsList.slice(0, index + 1)
    }

    this.breadcrumbs.next(this.breadcrumbsList)
  }

}
