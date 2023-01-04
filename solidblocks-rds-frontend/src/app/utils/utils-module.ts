import {NgModule} from "@angular/core";
import {StatusComponent} from "./status.component";
import {StatusColorPipe} from "./status-color.pipe";
import {StatusBackgroundColorPipe} from "./status-backgorund-color.pipe";

@NgModule({
  declarations: [
    StatusComponent,
    StatusColorPipe, StatusBackgroundColorPipe
  ],
  exports: [StatusBackgroundColorPipe, StatusComponent, StatusColorPipe],
  imports: []
})
export class UtilsModule {
}
