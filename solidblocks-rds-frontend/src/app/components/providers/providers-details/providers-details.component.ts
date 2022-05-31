import {Component, OnInit} from '@angular/core';
import {ProvidersService} from "../../../services/providers.service";
import {BaseFormComponent} from "../../base-form.component";
import {ToastService} from "../../../utils/toast.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Subscription} from "rxjs";
import {ProviderResponse} from "../../../services/types";
import {ContextService} from "../../navigation/context.service";
import {RdsInstancesService} from "../../../services/rds-instances.service";

@Component({
  selector: 'app-providers-details',
  templateUrl: './providers-details.component.html',
})
export class ProvidersDetailsComponent extends BaseFormComponent implements OnInit {

  private subscription: Subscription;

  provider: ProviderResponse

  rdsInstances: Array<ProviderResponse>

  constructor(private contextService: ContextService,
              private router: Router,
              private route: ActivatedRoute,
              private rdsInstancesService: RdsInstancesService,
              private providersService: ProvidersService,
              private toastsService: ToastService) {
    super(toastsService);
  }

  ngOnInit(): void {
    this.subscription = this.route.params.subscribe(
      (next) => {
        this.providersService.get(next['providerId']).subscribe(
          (next) => {
            this.provider = next.provider
            this.contextService.nextProvider(next.provider)
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
