import {Pipe, PipeTransform} from '@angular/core';
import {StatusResponse} from "../services/types";

@Pipe({name: 'statusBackgroundColor'})
export class StatusBackgroundColorPipe implements PipeTransform {
  transform(status: StatusResponse): string {
    switch (status.health) {
      case "ERROR":
        return "bg-danger";
      case "HEALTHY":
        return "bg-success";
      case "UNHEALTHY":
        return "bg-danger";
      default:
        return "bg-secondary";
    }
  }
}
