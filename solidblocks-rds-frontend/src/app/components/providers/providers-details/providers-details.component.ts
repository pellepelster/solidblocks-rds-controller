import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {ProvidersService} from "../../../services/providers.service";
import {BaseFormComponent} from "../../base-form.component";
import {ToastService} from "../../../utils/toast.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Subscription} from "rxjs";
import {ProviderResponse} from "../../../services/types";

@Component({
  selector: 'app-providers-details',
  templateUrl: './providers-details.component.html',
})
export class ProvidersDetailsComponent extends BaseFormComponent implements OnInit {

  private subscription: Subscription;

  provider: ProviderResponse;

  constructor(private router: Router, private route: ActivatedRoute, private providersService: ProvidersService, private toastsService: ToastService) {
    super(toastsService);
  }

  ngOnInit(): void {
    this.subscription = this.route.params.subscribe(
      (next) => {
        this.providersService.get(next['id']).subscribe(
          (next) => {
            this.provider = next.provider
          },
          (error) => {
            this.toastsService.handleErrorResponse(error)
          }
        )
      });
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
