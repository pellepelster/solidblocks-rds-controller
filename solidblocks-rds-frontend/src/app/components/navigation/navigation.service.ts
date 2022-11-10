import {Injectable} from '@angular/core';
import {Subject} from "rxjs";
import {ProviderResponse} from "../../services/types";

@Injectable({
  providedIn: 'root',
})
export class NavigationService {

  public currentProvider = new Subject<ProviderResponse>();

  selectProvider(provider: ProviderResponse) {
    this.currentProvider.next(provider)
  }

}
