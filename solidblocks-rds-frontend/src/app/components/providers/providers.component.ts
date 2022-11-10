import {Component} from '@angular/core';

@Component({
  selector: 'app-providers',

  template: `
    <div class="d-flex flex-grow-1 justify-content-center">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: []
})
export class ProvidersComponent {
}
