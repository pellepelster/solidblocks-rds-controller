import {Component, ComponentRef, Directive, OnInit, Type, ViewChild, ViewContainerRef} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {RdsInstancesService} from "../../../services/rds-instances.service";
import {BaseFormComponent} from "../../base-form.component";
import {ToastService} from "../../../utils/toast.service";
import {Router} from "@angular/router";
import {ContextService} from "../../navigation/context.service";

@Directive({
  selector: '[rdsInstanceWizardSteps]'
})
export class ProviderWizardStepsDirective {
  constructor(public viewContainerRef: ViewContainerRef) {
  }
}

interface RdsInstancesCreateStepComponent {
  stepIsValid(): boolean

  finish(): any
}

@Component({
  selector: 'rds-instance-wizard-step1',
  template: `
    <div>
      <ul class="list-group">
        <li class="list-group-item" [class.active]="selectedRdsInstance == rdsInstanceType.id"
            (click)="this.selectRdsInstanceType(rdsInstanceType.id)" *ngFor="let rdsInstanceType of rdsInstanceTypes">
          <img src="assets/{{rdsInstanceType.id}}.svg" width="64" alt="{{rdsInstanceType.description}}">
          {{rdsInstanceType.description}}
        </li>
      </ul>
    </div>
  `
})
export class RdsInstancesWizardStep1Component implements RdsInstancesCreateStepComponent {

  selectedRdsInstance: string | null

  rdsInstanceTypes = [
    {
      id: "postgresql",
      description: "PostgreSQL"
    }
  ]

  selectRdsInstanceType(id: string) {
    this.selectedRdsInstance = id
  }

  stepIsValid(): boolean {
    return this.selectedRdsInstance != null
  }

  finish() {
  }
}

@Component({
  selector: 'rds-instance-wizard-step2',
  template: `
    <div>
      <form [formGroup]="form" (ngSubmit)="finish()">

        <input-control [form]="form" [messages]="messages" formControlName="name"></input-control>

      </form>

    </div>
  `
})
export class RdsInstancesWizardStep2Component extends BaseFormComponent implements RdsInstancesCreateStepComponent {

  form = new FormGroup({
    name: new FormControl('', [
      Validators.required
    ])
  });

  constructor(private contextService: ContextService, private rdsInstancesService: RdsInstancesService, private router: Router, toastService: ToastService) {
    super(toastService);
  }

  finish() {

    if (!this.contextService.currentProviderId.value) {
      return
    }

    const providerId = this.contextService.currentProviderId.value
    this.rdsInstancesService.create(this.form.value.name, providerId).subscribe(
      (data) => {
        this.router.navigate(['providers', providerId, 'rds-instances', data.rdsInstance.id])
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
  selector: 'app-rds-instances-create',
  templateUrl: './rds-instances-create.component.html',
})
export class RdsInstancesCreateComponent extends BaseFormComponent implements OnInit {

  @ViewChild(ProviderWizardStepsDirective, {static: true}) private dynamicHost!: ProviderWizardStepsDirective;

  private steps: { type: Type<RdsInstancesCreateStepComponent> }[] = [
    {type: RdsInstancesWizardStep1Component},
    {type: RdsInstancesWizardStep2Component},
  ];

  currentStepIndex: number = 0
  currentStep: ComponentRef<RdsInstancesCreateStepComponent>

  constructor(private rdsInstancesService: RdsInstancesService, private router: Router, toastService: ToastService) {
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

    this.currentStep = viewContainerRef.createComponent<RdsInstancesCreateStepComponent>(this.steps[this.currentStepIndex].type);
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
