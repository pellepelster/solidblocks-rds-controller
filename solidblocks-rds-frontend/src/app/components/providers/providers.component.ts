import {Component} from '@angular/core';

@Component({
  selector: 'app-providers',
  template: `
    <div class="d-flex">
      <div class="d-flex flex-column flex-shrink-0 bg-white" style="width: 13em; border-right: #aaaaaa 2px solid;">
        <ul class="nav nav-pills flex-column mb-auto">
          <li>
            <a href="#" class="nav-link">
              <span class="bi bi-server"></span>RDS
            </a>
          </li>
        </ul>
      </div>

      <router-outlet name="providers"></router-outlet>
    </div>
  `
})
export class ProvidersComponent {

}
