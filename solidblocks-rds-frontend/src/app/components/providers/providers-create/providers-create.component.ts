import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {ProvidersService} from "../../../services/providers.service";
import {BaseFormComponent} from "../../base-form.component";
import {ToastService} from "../../../utils/toast.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-providers-create',
  templateUrl: './providers-create.component.html',
})
export class ProvidersCreateComponent extends BaseFormComponent implements OnInit {

  form = new FormGroup({
    name: new FormControl('', [
      Validators.required
    ]),
  });

  constructor(private providersService: ProvidersService, private router: Router, toastService: ToastService) {
    super(toastService);
  }

  ngOnInit(): void {
  }

  onSubmit() {
    this.providersService.create(this.form.value.name).subscribe(
      (data) => {
        this.router.navigate(['providers', data.provider.id])
      },
      (error) => {
        this.handleErrorResponse(error)
      },
    )
  }
}
