import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {ToastService} from "../../../utils/toast.service";
import {RdsInstancesService} from "../../../services/rds-instances.service";
import {ProviderResponse} from "../../../services/types";

@Component({
  selector: 'app-rds-instances-home',
  templateUrl: './rds-instances-home.component.html',
})
export class RdsInstancesHomeComponent implements OnInit, OnDestroy {

  providers: Array<ProviderResponse>

  constructor(private route: ActivatedRoute, private providersService: RdsInstancesService, private toastsService: ToastService) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy() {
  }

}
