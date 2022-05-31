import {Component, OnInit} from '@angular/core';
import {ContextService} from "./context.service";
import {BehaviorSubject} from "rxjs";
import {ProviderResponse, RdsInstanceResponse} from "../../services/types";

@Component({
  selector: 'app-navigation',
  templateUrl: './navigation.component.html',
})
export class NavigationComponent implements OnInit {

  currentProvider: BehaviorSubject<ProviderResponse | null>
  currentRdsInstance: BehaviorSubject<RdsInstanceResponse | null>

  constructor(private contextService: ContextService) {
  }

  ngOnInit(): void {
    this.currentProvider = this.contextService.currentProvider
    this.currentRdsInstance = this.contextService.currentRdsInstance
  }

}
