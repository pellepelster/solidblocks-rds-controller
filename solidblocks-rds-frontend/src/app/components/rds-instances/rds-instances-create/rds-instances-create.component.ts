import {Component, ComponentRef, Directive, OnInit, Type, ViewChild, ViewContainerRef} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {RdsInstancesService} from "../../../services/rds-instances.service";
import {BaseFormComponent} from "../../base-form.component";
import {ToastService} from "../../../utils/toast.service";
import {ActivatedRoute, Router} from "@angular/router";
import {NavigationService} from "../../navigation/navigation.service";

@Directive({
  selector: '[rdsInstanceWizardSteps]'
})
export class ProviderWizardStepsDirective {
  constructor(public viewContainerRef: ViewContainerRef) {
  }
}

interface RdsInstancesCreateStepComponent {

  providerId: string

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

  providerId: string

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

        <input-control [form]="form" [messages]="messages" formControlLabel="name"
                       formControlName="name"></input-control>
        <input-control [form]="form" [messages]="messages" formControlLabel="username"
                       formControlName="username"></input-control>
        <input-control [form]="form" [messages]="messages" formControlLabel="password"
                       formControlName="password"></input-control>

      </form>

    </div>
  `
})
export class RdsInstancesWizardStep2Component extends BaseFormComponent implements RdsInstancesCreateStepComponent {

  providerId: string

  form = new FormGroup({
    name: new FormControl('', [
      Validators.required
    ]),
    username: new FormControl('', [
      Validators.required
    ]),
    password: new FormControl('', [
      Validators.required
    ])
  });

  constructor(private navigationService: NavigationService, private rdsInstancesService: RdsInstancesService, private router: Router, toastService: ToastService) {
    super(toastService);
  }

  finish() {
    this.rdsInstancesService.create(this.form.value.name as string, this.providerId, this.form.value.username as string, this.form.value.password as string).subscribe(
      (data) => {
        this.router.navigate(['providers', this.providerId])
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

  providerId: string

  currentStepIndex: number = 0
  currentStep: ComponentRef<RdsInstancesCreateStepComponent>

  constructor(private rdsInstancesService: RdsInstancesService, private route: ActivatedRoute, private router: Router, toastService: ToastService) {
    super(toastService);

    this.route.parent!.params.subscribe(
      (params) => {
        this.providerId = params['providerId']
      })
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

    this.currentStep.instance.providerId = this.providerId

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
