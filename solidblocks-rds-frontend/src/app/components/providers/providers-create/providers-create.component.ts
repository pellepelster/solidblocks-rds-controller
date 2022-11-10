import {Component, ComponentRef, Directive, OnInit, Type, ViewChild, ViewContainerRef} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {ProvidersService} from "../../../services/providers.service";
import {BaseFormComponent} from "../../base-form.component";
import {ToastService} from "../../../utils/toast.service";
import {Router} from "@angular/router";

@Directive({
  selector: '[providersWizardSteps]'
})
export class ProviderWizardStepsDirective {
  constructor(public viewContainerRef: ViewContainerRef) {
  }
}

interface ProvidersCreateStepComponent {
  stepIsValid(): boolean

  finish(): any
}

@Component({
  selector: 'providers-wizard-step1',
  template: `
    <p>Select the type of the provider to create</p>
    <ul class="list-group">
      <li class="list-group-item" [class.active]="selectedProvider == providerType.id"
          (click)="this.selectProvider(providerType.id)" *ngFor="let providerType of providerTypes">
        <img src="assets/{{providerType.id}}.svg" width="64" alt="{{providerType.description}}">
        {{providerType.description}}
      </li>
    </ul>
  `
})
export class ProvidersWizardStep1Component implements ProvidersCreateStepComponent {

  selectedProvider: string | null

  providerTypes = [
    {
      id: "hetzner-cloud",
      description: "Hetzner Cloud"
    }
  ]

  selectProvider(id: string) {
    this.selectedProvider = id
  }

  stepIsValid(): boolean {
    return this.selectedProvider != null
  }

  finish() {
  }
}

@Component({
  selector: 'providers-wizard-step2',
  template: `
    <p>Select the type of the provider to create</p>
    <form [formGroup]="form" (ngSubmit)="finish()">

      <input-control [form]="form" [messages]="messages" formControlLabel="Name" formControlName="name"></input-control>
      <input-control [form]="form" [messages]="messages" formControlLabel="API Token"
                     formControlName="apiKey"></input-control>

    </form>
  `
})
export class ProvidersWizardStep2Component extends BaseFormComponent implements ProvidersCreateStepComponent {

  form = new FormGroup({
    name: new FormControl('', [
      Validators.required
    ]),
    apiKey: new FormControl('', [
      Validators.required
    ]),
  })

  constructor(private providersService: ProvidersService, private router: Router, toastService: ToastService) {
    super(toastService);
  }

  finish() {
    this.providersService.create(this.form.value.name as string, this.form.value.apiKey as string).subscribe(
      (data) => {
        this.router.navigate(['providers', data.provider.id])
      },
      (error) => {
        this.handleErrorResponse(error)
      }
    )
  }

  stepIsValid(): boolean {
    return this.form.valid;
  }
}


@Component({
  selector: 'app-providers-create',
  templateUrl: './providers-create.component.html',
})
export class ProvidersCreateComponent extends BaseFormComponent implements OnInit {

  @ViewChild(ProviderWizardStepsDirective, {static: true}) private dynamicHost!: ProviderWizardStepsDirective;

  private steps: { type: Type<ProvidersCreateStepComponent> }[] = [
    {type: ProvidersWizardStep1Component},
    {type: ProvidersWizardStep2Component},
  ];

  currentStepIndex: number = 0
  currentStep: ComponentRef<ProvidersCreateStepComponent>

  constructor(private providersService: ProvidersService, private router: Router, toastService: ToastService) {
    super(toastService);
  }

  hasNextStep() {
    return this.currentStepIndex < this.steps.length - 1
  }

  nextStep() {
    if (this.hasNextStep()) {
      this.currentStepIndex++
    }
    this.loadStep()
  }

  hasPreviousStep() {
    return this.currentStepIndex > 0
  }

  previousStep() {
    if (this.hasPreviousStep()) {
      this.currentStepIndex--
    }
    this.loadStep()
  }

  ngOnInit(): void {
    this.loadStep()
  }

  loadStep(): void {

    const viewContainerRef = this.dynamicHost.viewContainerRef;
    viewContainerRef.clear();

    this.currentStep = viewContainerRef.createComponent<ProvidersCreateStepComponent>(this.steps[this.currentStepIndex].type);
  }

  finish() {
    if (this.currentStep) {
      return this.currentStep.instance.finish();
    }
  }

  currentStepValid() {
    if (this.currentStep) {
      return this.currentStep.instance.stepIsValid();
    }

    return false
  }
}
