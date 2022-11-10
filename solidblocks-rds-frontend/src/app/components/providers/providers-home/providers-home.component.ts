import {Component, OnInit} from '@angular/core';
import {ProvidersService} from "../../../services/providers.service";
import {BaseFormComponent} from "../../base-form.component";
import {ToastService} from "../../../utils/toast.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Subscription} from "rxjs";
import {ProviderResponse, RdsInstanceResponse} from "../../../services/types";
import {NavigationService} from "../../navigation/navigation.service";
import {RdsInstancesService} from "../../../services/rds-instances.service";

@Component({
  selector: 'app-providers-details',
  templateUrl: './providers-home.component.html',
})
export class ProvidersHomeComponent extends BaseFormComponent implements OnInit {

  private subscription: Subscription;

  provider: ProviderResponse

  rdsInstances: Array<RdsInstanceResponse>

  constructor(private navigationService: NavigationService,
              private router: Router,
              private route: ActivatedRoute,
              private rdsInstancesService: RdsInstancesService,
              private providersService: ProvidersService,
              private toastsService: ToastService) {
    super(toastsService);
  }

  ngOnInit(): void {

    this.subscription = this.route.params.subscribe(
      (params) => {
        this.providersService.get(params['providerId']).subscribe(
          (data) => {
            this.provider = data.provider
            this.navigationService.selectProvider(data.provider)
          },
          (error) => {
            this.toastsService.handleErrorResponse(error)
          }
        )
      })

    this.rdsInstancesService.list().subscribe(
      (response) => {
        this.rdsInstances = response.rdsInstances
      },
      (error) => {
        this.toastsService.handleErrorResponse(error)
      }
    )
  }

  delete(id: string) {
    this.providersService.delete(id).subscribe((next) => {
        this.router.navigate(['providers'])
      },
      (error) => {
        this.toastsService.handleErrorResponse(error)
      }
    )
  }
}
