import {Injectable} from '@angular/core';
import {Subject} from "rxjs";
import {ProviderResponse} from "../../services/types";

@Injectable({
  providedIn: 'root',
})
export class NavigationService {

  public currentProvider = new Subject<ProviderResponse | null>();

  selectProvider(provider: ProviderResponse | null) {
    this.currentProvider.next(provider)
  }

}
