import {Pipe, PipeTransform} from '@angular/core';

@Pipe({name: 'providerStatusColor'})
export class ProviderStatusColorPipe implements PipeTransform {
  transform(value: string): string {

    switch (value) {
      case "UNKNOWN":
        return "light";
      case "ERROR":
        return "danger";
      case "HEALTHY":
        return "success";
      case "UNHEALTHY":
        return "danger";
      default:
        return "secondary";
    }
  }
}
