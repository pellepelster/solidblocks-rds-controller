import {Pipe, PipeTransform} from '@angular/core';
import {StatusResponse} from "../services/types";

@Pipe({name: 'statusColor'})
export class StatusColorPipe implements PipeTransform {

  transform(status: StatusResponse): string {
    switch (status.health) {
      case "UNKNOWN":
        return "text-dark";
      default:
        return "text-light";
    }
  }
}
