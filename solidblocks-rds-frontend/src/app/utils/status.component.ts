import {Component, Input} from '@angular/core';
import {StatusResponse} from "../services/types";

@Component({
  selector: 'app-status',
  template: `<span
    class="badge {{ status | statusColor}} {{ status | statusBackgroundColor}} ml-2">{{status.health}}</span>`
})
export class StatusComponent {

  @Input()
  status: StatusResponse
}
